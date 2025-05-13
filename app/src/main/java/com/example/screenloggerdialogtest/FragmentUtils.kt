package com.example.screenloggerdialogtest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class FragmentUtils {

    fun createOnboardingFragment(
        fragment: Fragment,
        inflater: LayoutInflater,
        container: ViewGroup?,
        studyStateVars: Map<String, Int>
    ): View {
        val context = fragment.requireContext()
        val activity = fragment.requireActivity()
        val layout = inflater.inflate(R.layout.settings_layout, container, false)



        // Now continue using `context`, `activity`, `layout`, etc.
        return layout
    }

}