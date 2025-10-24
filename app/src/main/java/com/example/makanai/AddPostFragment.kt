package com.example.makanai

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class AddPostFragment : Fragment() {

    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout
    private var stepCounter = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ingredientsContainer = view.findViewById(R.id.ingredients_container)
        stepsContainer = view.findViewById(R.id.steps_container)
        val addIngredientButton: TextView = view.findViewById(R.id.add_ingredient_button)
        val addStepButton: TextView = view.findViewById(R.id.add_step_button)
        val backButton: ImageButton = view.findViewById(R.id.back_button_add_post)
        val postButtonTop: Button = view.findViewById(R.id.post_button_top)
        val publishButtonBottom: Button = view.findViewById(R.id.publish_button_bottom)
        val categorySpinner: Spinner = view.findViewById(R.id.category_spinner)
        val difficultySpinner: Spinner = view.findViewById(R.id.difficulty_spinner)

        setupSpinner(categorySpinner, R.array.recipe_categories)
        setupSpinner(difficultySpinner, R.array.recipe_difficulties)

        addIngredientButton.setOnClickListener { addIngredientInput() }
        addStepButton.setOnClickListener { addStepInput() }

        // Setup initial fields BUT hide delete button if only one
        setupInitialField(ingredientsContainer.getChildAt(0), ingredientsContainer, false)
        setupInitialField(stepsContainer.getChildAt(0), stepsContainer, true)

        backButton.setOnClickListener { findNavController().popBackStack() }
        val postAction = {
            // TODO: Collect data
            findNavController().popBackStack()
        }
        postButtonTop.setOnClickListener { postAction() }
        publishButtonBottom.setOnClickListener { postAction() }
    }

    private fun setupSpinner(spinner: Spinner, arrayResourceId: Int) {
        ArrayAdapter.createFromResource(
            requireContext(), arrayResourceId, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    // NEW function to set up the very first field (hides delete button)
    private fun setupInitialField(view: View, container: LinearLayout, isStep: Boolean) {
        val deleteButtonId = if (isStep) R.id.delete_step_button else R.id.delete_ingredient_button
        val deleteButton: ImageButton = view.findViewById(deleteButtonId)

        // Hide delete button initially
        deleteButton.visibility = View.GONE

        // Set up the listener for when it might become visible later
        deleteButton.setOnClickListener {
            handleDeleteAction(view, container, isStep)
        }
    }

    private fun addIngredientInput() {
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val ingredientView = inflater.inflate(R.layout.item_ingredient_input, ingredientsContainer, false)

        setupDynamicDeleteButton(ingredientView, ingredientsContainer, false) // Setup delete for the new item

        // If this is the *second* item being added, make the delete button on the *first* item visible
        if (ingredientsContainer.childCount == 1) {
            val firstItemView = ingredientsContainer.getChildAt(0)
            firstItemView.findViewById<ImageButton>(R.id.delete_ingredient_button)?.visibility = View.VISIBLE
        }

        ingredientsContainer.addView(ingredientView)
    }

    private fun addStepInput() {
        stepCounter = stepsContainer.childCount + 1 // Calculate next step number based on current count
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val stepView = inflater.inflate(R.layout.item_step_input, stepsContainer, false)

        val stepNumberTextView: TextView = stepView.findViewById(R.id.step_number)
        stepNumberTextView.text = stepCounter.toString()

        setupDynamicDeleteButton(stepView, stepsContainer, true) // Setup delete for the new item

        // If this is the *second* step being added, make the delete button on the *first* step visible
        if (stepsContainer.childCount == 1) {
            val firstItemView = stepsContainer.getChildAt(0)
            firstItemView.findViewById<ImageButton>(R.id.delete_step_button)?.visibility = View.VISIBLE
        }

        stepsContainer.addView(stepView)
    }

    // NEW function specifically for delete buttons added dynamically (always visible)
    private fun setupDynamicDeleteButton(view: View, container: LinearLayout, isStep: Boolean) {
        val deleteButtonId = if (isStep) R.id.delete_step_button else R.id.delete_ingredient_button
        val deleteButton: ImageButton = view.findViewById(deleteButtonId)

        deleteButton.visibility = View.VISIBLE // Dynamically added buttons are always visible

        deleteButton.setOnClickListener {
            handleDeleteAction(view, container, isStep)
        }
    }

    // NEW consolidated delete logic
    private fun handleDeleteAction(viewToRemove: View, container: LinearLayout, isStep: Boolean) {
        container.removeView(viewToRemove)
        // If only one item remains after deletion, hide its delete button
        if (container.childCount == 1) {
            val lastItemView = container.getChildAt(0)
            val lastDeleteButtonId = if (isStep) R.id.delete_step_button else R.id.delete_ingredient_button
            lastItemView.findViewById<ImageButton>(lastDeleteButtonId)?.visibility = View.GONE
        }

        // Renumber steps if a step was removed
        if (isStep) {
            renumberSteps()
        }
    }


    private fun renumberSteps() {
        // Renumber based on the new order in the container
        for (i in 0 until stepsContainer.childCount) {
            val stepView = stepsContainer.getChildAt(i)
            val stepNumberTextView: TextView = stepView.findViewById(R.id.step_number)
            stepNumberTextView.text = (i + 1).toString() // Set number based on index + 1
        }
        stepCounter = stepsContainer.childCount // Update the counter for the next add
    }

    // TODO: Add function to collect data
}