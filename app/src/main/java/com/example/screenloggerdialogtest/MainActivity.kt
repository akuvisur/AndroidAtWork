package com.example.screenloggerdialogtest

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.math.min

class MainActivity : FragmentActivity() {

    lateinit var mainAdapter : ViewPagerAdapter
    lateinit var viewPager : ViewPager2

    companion object {
        const val REQUEST_CODE_OVERLAY = 100 // Arbitrary value for overlay permission callback
        const val REQUEST_CODE_NOTIFICATION = 200 // value for notification permission callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

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
                    tab.text = "Settings"
                }
            }
        }.attach()

        FirebaseUtils.authenticateUser(
            onSuccess = { uid ->
                //Toast.makeText(this, "Authentication succeeded! UID: $uid", Toast.LENGTH_SHORT).show()
            },
            onFailure = { errorMessage ->
                //Toast.makeText(this, "Authentication failed: $errorMessage", Toast.LENGTH_LONG).show()
                Log.d("firebase", errorMessage.toString())
            }
        )
    }

    private var lastResume : Long = 0
    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
    }

    fun refreshUI() {
        this.recreate()
    }

    fun setViewPagerPage(page : Int) {
        viewPager.currentItem = page
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Toast.makeText(this, "Returning to this activity", Toast.LENGTH_SHORT).show()

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
        private lateinit var inflaterView : View

        private lateinit var workHourButton : Button
        private lateinit var studyProgressSlider: Slider

        @SuppressLint("NewApi", "DefaultLocale")
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            // go to settings tab if they are not set correctly

            Toast.makeText(requireContext(), "Checking settings, please wait..", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                if (!checkSettingsAndRun()) {
                    (activity as? MainActivity)?.setViewPagerPage(2)
                    Toast.makeText(requireContext(), "Please make sure all settings are correctly enabled", Toast.LENGTH_LONG).show()
                }
                else {
                    Toast.makeText(requireContext(), "Settings OK", Toast.LENGTH_SHORT).show()
                }
            }, 5000)

            // Actions for baseline ongoing
            // e.g., initialize baseline tracking, display tracking progress
            inflaterView = inflater.inflate(R.layout.data_collection_ongoing, container, false)

            /*
            workHourButton = inflaterView.findViewById(R.id.editWorkTimeButton)
            workHourButton.setOnClickListener {
                val intent = Intent(requireContext(), WorkTimeSelectorActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }*/

            studyProgressSlider = inflaterView.findViewById(R.id.studyProgressSlider)
            studyProgressSlider.value = 1F

            val studyDay = calculateStudyDay(requireContext())
            if (studyDay > 0) {
                studyProgressSlider.value = min(studyDay, 7).toFloat()
            }

            val bottomText = inflaterView.findViewById<TextView>(R.id.bottomText)

            if (studyDay > 7) {
                bottomText.text = "* Study is now complete."
            } else {
                bottomText.text = "* Please complete seven days of data collection."
            }

            /*
            val selectedWorkHoursText = inflaterView.findViewById<TextView>(R.id.selectedWorkHoursText)
            // Check if there are any selected work hours
            val savedIntervals = getWorkIntervals(context)

            if (savedIntervals.isEmpty()) {
                // No work hours are selected, show instruction text
                selectedWorkHoursText.text = "Please add work hours by clicking the button below."
            } else {
                // Work hours are selected, show current selected work hours
                selectedWorkHoursText.text = "You can edit your work hours by clicking the button below"
            }

             */

            return inflaterView
        }

        private fun checkSettingsAndRun(): Boolean {

            if (context == null || !isAdded) {
                // Fragment is not attached or context is null, skip operation
                return false
            }
            val c = requireContext()
            // 1. Accessibility
            if (!isAccessibilityServiceEnabled(c, ApplicationTrackingService::class.java)) {
                stopService(c, BaselineService::class.java)
                return false
            }

            // 2. Notification permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(c, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    stopService(c, BaselineService::class.java)
                    return false
                }
            }

            // 3. Overlay permission
            if (!Settings.canDrawOverlays(c)) {
                stopService(c, BaselineService::class.java)
                return false
            }

            checkServicesRunning(requireContext())
            checkAndStartService(BaselineService::class.java)

            return true
        }

        fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
            val expectedComponentName = ComponentName(context, serviceClass)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)

            return colonSplitter.any { ComponentName.unflattenFromString(it) == expectedComponentName }
        }

        private fun checkAndStartService(serviceClass: Class<*>) {
            if (!isServiceRunning(requireContext(), serviceClass)) {
                val intent = Intent(
                    requireContext(),
                    serviceClass
                ) // Build the intent for the service
                (requireActivity() as MainActivity).startForegroundService(intent)
            }
        }

        private fun stopService(context: Context, serviceClass: Class<*>) {
            val intent = Intent(context, serviceClass)
            context.stopService(intent)
            Log.d("ServiceManager", "${serviceClass.simpleName} asked to be stopped (is not necessarily running).")
        }

    }

    // settings fragment is always the same
    // some fields are disabled field.isEnabled = false prior to INT1
    class SettingsFragment : Fragment() {

        // Fragment-level variables for UI elements
        private lateinit var notificationButton: Button
        private lateinit var notificationIcon: ImageView
        private lateinit var batteryButton: Button
        private lateinit var batteryIcon: ImageView
        private lateinit var overlayButton: Button
        private lateinit var overlayIcon: ImageView
        private lateinit var accessibilityServiceText : TextView
        private lateinit var accessibilityButton: Button
        private lateinit var accessibilityIcon: ImageView
        private lateinit var participantInput: EditText
        private lateinit var startServicesButton: Button

        private lateinit var testDialogButton : Button

        @SuppressLint("StringFormatInvalid", "DefaultLocale")
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.settings_layout, container, false)

            // Initialize UI elements
            notificationButton = view.findViewById(R.id.allowNotificationButton)
            notificationIcon = view.findViewById(R.id.allowNotificationIcon)
            batteryButton = view.findViewById(R.id.disableBatteryManagementButton)
            batteryIcon = view.findViewById(R.id.allowBattery)
            overlayButton = view.findViewById(R.id.overlayRequestButton)
            overlayIcon = view.findViewById(R.id.allowOverlayImageView)
            accessibilityServiceText = view.findViewById(R.id.accessibilityServiceText)
            accessibilityButton = view.findViewById(R.id.accessibilityServiceRequestButton)
            accessibilityIcon = view.findViewById(R.id.allowAccessibilityImageView)
            participantInput = view.findViewById(R.id.participantIdInput)
            startServicesButton = view.findViewById(R.id.startServicesButton)
            testDialogButton = view.findViewById(R.id.settings_test_dialog_button)

            // Set participant ID if available
            participantInput.setText(getParticipantId(requireContext()))

            // Set OnFocusChangeListener for participant input
            participantInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val input = participantInput.text.toString().trim()
                    if (input.isNotEmpty()) {
                        saveParticipantId(requireContext(), input)

                        val path = "/users/${FirebaseUtils.getCurrentUserUID()}/participant_id/${System.currentTimeMillis()}"
                        val data = FirebaseUtils.FirebaseParticipantIdObject(participantId = input)
                        FirebaseUtils.uploadFirebaseEntry(path, data)
                    }
                }
            }

            return view
        }

        override fun onResume() {
            super.onResume()

            // === NOTIFICATION PERMISSION ===
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    requireContext(), android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                notificationIcon.visibility = if (granted) View.VISIBLE else View.INVISIBLE
                notificationButton.alpha = if (granted) 1.0f else 0.5f
            } else {
                // Permission not needed on Android 12 and below
                notificationIcon.visibility = View.VISIBLE
            }

            notificationButton.setOnClickListener {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }

            // === BATTERY OPTIMIZATION ===
            batteryIcon.visibility = View.VISIBLE
            batteryIcon.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Due to Android limitations, this app cannot verify if battery optimizations are disabled. Please check manually in settings.",
                    Toast.LENGTH_LONG
                ).show()
            }

            batteryButton.setOnClickListener {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }

            // === OVERLAY PERMISSION ===
            overlayIcon.visibility = if (Settings.canDrawOverlays(requireContext())) View.VISIBLE else View.INVISIBLE
            overlayButton.setOnClickListener {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}"))
                startActivity(intent)
            }

            // === ACCESSIBILITY SERVICE ===
            if (isAccessibilityServiceEnabled(requireContext(), ApplicationTrackingService::class.java)) {
                // If the service is enabled, check if the notification is visible
                accessibilityServiceText.text = "Accessibility service is ENABLED but may not be ACTIVE unless the 'Tracking foreground apps' notification is visible. Please double-check that you see the notification. If you do not see it, please turn the accessibility service off and on again."
                accessibilityServiceText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            } else {
                // If the service is not enabled
                accessibilityServiceText.text = "Accessibility services is required for application tracking. Please enable the service in accessibility settings ."
            }

            if (isAccessibilityServiceEnabled(requireContext(), ApplicationTrackingService::class.java)) {
                accessibilityIcon.visibility = View.VISIBLE
            } else {
                accessibilityIcon.visibility = View.INVISIBLE
            }

            accessibilityButton.setOnClickListener {
                Toast.makeText(requireContext(), "Please allow accessibility service. If you do not see a notification appear, try turning the switch off and back on.", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }

            // === START SERVICES BUTTON ===
            startServicesButton.setOnClickListener {
                // Disable the button and start animation
                startServicesButton.isEnabled = false
                val fadeOut = AlphaAnimation(1f, 0.5f)
                fadeOut.duration = 150
                fadeOut.fillAfter = true
                startServicesButton.startAnimation(fadeOut)

                // Start the service
                val intent = Intent(requireContext(), BaselineService::class.java)
                (requireActivity() as MainActivity).startForegroundService(intent)

                // Re-enable the button after 1 second
                startServicesButton.postDelayed({
                    if (participantInput.text.isEmpty()) {
                        Toast.makeText(requireContext(), "Input participant number", Toast.LENGTH_SHORT).show()
                    } else if (isServiceRunning(requireContext(), BaselineService::class.java)) {
                        if (isAccessibilityServiceEnabled(requireContext(), ApplicationTrackingService::class.java)) {
                            Toast.makeText(requireContext(), "Everything seems OK - you should now see two notifications from AndroidAtWork", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(), "Accessibility service not enabled.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Baseline service not active. Check notification permission.", Toast.LENGTH_SHORT).show()
                    }

                    startServicesButton.isEnabled = true
                    val fadeIn = AlphaAnimation(0.5f, 1f)
                    fadeIn.duration = 150
                    fadeIn.fillAfter = true
                    startServicesButton.startAnimation(fadeIn)

                    (activity as? MainActivity)?.setViewPagerPage(1)

                }, 1000) // 1000ms delay
            }

            testDialogButton.setOnClickListener {
                val dialog = UnlockDialog()
                dialog.showDialog(context, System.currentTimeMillis(), 0, 0, 1, DIALOG_TYPE_USAGE_INITIATED, false)
            }
        }

        private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
            val expectedId = "${context.packageName}/${serviceClass.name}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(':').any { it.equals(expectedId, ignoreCase = true) }
        }

    }


}
