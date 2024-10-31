package com.example.screenloggerdialogtest

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : FragmentActivity() {

    private var study_start_ts = 0
    private var study_state : Int = 0

    private lateinit var mainAdapter : ViewPagerAdapter
    private lateinit var viewPager : ViewPager2

    private lateinit var studyPhaseSlider : Slider
    private lateinit var studyPhaseText : TextView
    private var studyPhaseSliderValue : Float = 0.0f

    companion object {
        const val REQUEST_CODE_OVERLAY = 100 // Arbitrary value for overlay permission callback
        const val REQUEST_CODE_NOTIFICATION = 200 // value for notification permission callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("states", Context.MODE_PRIVATE)
        study_start_ts = sharedPrefs.getInt("study_start_ts", 0)

        setContentView(R.layout.activity_main)

        studyPhaseSlider = findViewById(R.id.studyPhaseSlider)
        studyPhaseSlider.isEnabled = false
        studyPhaseText = findViewById(R.id.studyPhaseText)

        viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        mainAdapter = ViewPagerAdapter(this)
        viewPager.adapter = mainAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Info"
                }
                1 -> {
                    tab.text = "Data"
                }
                2 -> {
                    tab.text = "Settings"
                }
            }
        }.attach()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // this launches the permission request
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_NOTIFICATION)
            }
        } else {
            true // For Android versions below 13, permissions are granted by default
        }

        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, BaselineService::class.java) // Build the intent for the service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            }
            else {
                Log.d("MAIN:ONCREATE", "SDK level too low")
            }
        } else {
            // this prompts user to the settings screen to enable overlay
            requestOverlayPermission()
        }

        /*
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, 100)
        }
        */

        enableEdgeToEdge()

    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onResume() {
        super.onResume()

        study_state = getStudyState(this)
        var study_day = 123

        studyPhaseText.text = "Study day $study_day: " + when (study_state) {
            STUDY_STATE_FIRST_LAUNCH -> "Study start"
            STUDY_STATE_CONSENT_GIVEN -> "Consent given"
            STUDY_STATE_BASELINE_ONGOING -> "Baseline phase started"
            STUDY_STATE_POST_BASELINE -> "User input required before Intervention #1"
            STUDY_STATE_INT1 -> "Intervention 1 ongoing"
            STUDY_STATE_POST_INT1 -> "User input required before Intervention #2"
            STUDY_STATE_INT2 -> "Intervention 2 ongoing"
            STUDY_STATE_POST_INT2_SURVEY_REQUIRED -> "Post Intervention 2 - Survey responses required"
            STUDY_STATE_COMPLETE -> "Study complete"
            else -> "Unknown state"
        }
        // TODO: Add day counter

        studyPhaseSlider.value = study_state.toFloat()
        studyPhaseSliderValue = when {
            study_state < STUDY_STATE_BASELINE_ONGOING -> 0f
            study_state < STUDY_STATE_INT1 -> 1f
            study_state < STUDY_STATE_INT2 -> 2f
            study_state <= STUDY_STATE_COMPLETE -> 3f
            else -> 0f
        }
        // TODO: Add daily calculation of value to show progress

        mainAdapter.notifyDataSetChanged()
        val sharedPrefs = getSharedPreferences("states", Context.MODE_PRIVATE)
        // always update study info values
        study_start_ts = sharedPrefs.getInt("study_start_ts", 0)

    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, now show the overlay
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Overlay permission is required.", Toast.LENGTH_SHORT).show()
            }
        }
        else if (requestCode == REQUEST_CODE_NOTIFICATION) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this, "Notifications required", Toast.LENGTH_SHORT).show()
        }
    }

    class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        private val fragments: List<Fragment> = listOf(
            InfoFragment(),
            DataCollectedFragment(),
            SettingsFragment()
        )

        override fun getItemCount(): Int {
            return fragments.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }
    }

    class InfoFragment : Fragment() {

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val inflaterView = when (getStudyState(requireContext())) {
                STUDY_STATE_FIRST_LAUNCH -> inflater.inflate(R.layout.first_launch_layout, container, false)
                STUDY_STATE_CONSENT_GIVEN -> inflater.inflate(R.layout.consent_given_layout, container, false)
                /*
                STUDY_STATE_BASELINE_ONGOING -> inflater.inflate(R.layout.baseline_ongoing_layout, container, false)

                STUDY_STATE_POST_BASELINE -> inflater.inflate(R.layout.post_baseline_layout, container, false)
                STUDY_STATE_INT1 -> inflater.inflate(R.layout.intervention1_layout, container, false)
                STUDY_STATE_POST_INT1 -> inflater.inflate(R.layout.post_int1_layout, container, false)
                STUDY_STATE_INT2 -> inflater.inflate(R.layout.intervention2_layout, container, false)
                STUDY_STATE_POST_INT2_SURVEY_REQUIRED -> inflater.inflate(R.layout.post_int2_survey_required_layout, container, false)
                STUDY_STATE_COMPLETE -> inflater.inflate(R.layout.complete_layout, container, false)
                */
                else -> inflater.inflate(R.layout.fragment_info, container, false) // Optional: Default case if needed
            }

            return inflater.inflate(R.layout.fragment_info, container, false)
        }
    }

    class DataCollectedFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            var inflaterView = if (getStudyState(requireContext()) < STUDY_STATE_INT1) {
                // no data shown before intervention 1
                inflater.inflate(R.layout.fragment_data_collected, container, false)
            }
            else {
                // data visualisation after intervention 1
                inflater.inflate(R.layout.fragment_data_collected, container, false)
            }
            // Inflate the layout for this fragment
            return inflaterView
        }
    }

    // settings fragment is always the same
    // some fields are disabled field.isEnabled = false prior to INT1
    class SettingsFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            // Inflate the layout for this fragment
            val view = inflater.inflate(R.layout.fragment_settings, container, false)

            // Find the button using the inflated view
            val startOnboardingButton: Button = view.findViewById(R.id.startOnBoardingButton)
            startOnboardingButton.setOnClickListener {
                // Start the MainActivity for onboarding
                val intent = Intent(activity, OnboardingActivity::class.java)
                startActivity(intent)
            }

            val spaiButton: Button = view.findViewById(R.id.SPAI)
            spaiButton.setOnClickListener {
                // Start the MainActivity for onboarding
                val intent = Intent(activity, SPAIActivity::class.java)
                startActivity(intent)
            }

            return view
        }
    }
}
