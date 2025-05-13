package com.example.screenloggerdialogtest

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.screenloggerdialogtest.FirebaseUtils.getCurrentUserUID
import com.example.screenloggerdialogtest.FirebaseUtils.uploadFirebaseEntry
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

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

        FirebaseUtils.authenticateUser(
            onSuccess = { uid ->
                //Toast.makeText(this, "Authentication succeeded! UID: $uid", Toast.LENGTH_SHORT).show()
            },
            onFailure = { errorMessage ->
                //Toast.makeText(this, "Authentication failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

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

    }

    fun refreshUI() {
        this.recreate()
    }

   private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
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

        private lateinit var worktimeButton : Button

        private lateinit var allowNotificationButton: Button
        private lateinit var disableBatteryManagementButton: Button

        private lateinit var allowNotificationIcon: ImageView

        private lateinit var studyProgressSlider: Slider

        private lateinit var enableOverlayButton: Button
        private  lateinit var intervention1TestButton: Button

        private val POST_NOTIFICATIONS_PERMISSION : String = "android.permission.POST_NOTIFICATIONS"

        @SuppressLint("NewApi", "DefaultLocale")
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {

            checkServicesRunning(requireContext())
            // go to settings tab if they are not set correctly

            Toast.makeText(requireContext(), "Checking settings..", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                if (!checkSettingsAndRun()) {
                    (activity as? MainActivity)?.setViewPagerPage(2)
                    Toast.makeText(requireContext(), "Please make sure all settings are correctly enabled", Toast.LENGTH_LONG).show()
                }
            }, 1000)

            // Actions for baseline ongoing
            // e.g., initialize baseline tracking, display tracking progress
            inflaterView = inflater.inflate(R.layout.data_collection_ongoing, container, false)

            worktimeButton = inflaterView.findViewById(R.id.editWorkTimeButton)
            worktimeButton.setOnClickListener {
                val intent = Intent(requireContext(), WorkTimeSelectorActivity::class.java)
                startActivity(intent)
            }
            /*
            val baselineDay = calculateStudyPeriodDay(BASELINE_START_TIMESTAMP, requireContext())
            if (baselineDay > BASELINE_DURATION) {
                setStudyState(requireContext(), STUDY_STATE_POST_BASELINE)
                (requireActivity() as MainActivity).refreshUI()
            }

             */

            checkAndStartService(BaselineService::class.java)

            studyProgressSlider = inflaterView.findViewById(R.id.studyProgressSlider)
            //studyProgressSlider.value = min(baselineDay.toFloat(), BASELINE_DURATION.toFloat())
            studyProgressSlider.value = 1F

            return inflaterView
        }

        private fun checkSettingsAndRun(): Boolean {

            if (!isAccessibilityServiceEnabled(requireContext(), ApplicationTrackingService::class.java)) {
                return false
            }
            // TODO check notification permission
            // TODO check overlay permission

            // TODO If everything is OK run services

            return false
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

        @SuppressLint("StringFormatInvalid", "DefaultLocale")
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            // Inflate the layout for this fragment
            val view = inflater.inflate(R.layout.settings_layout, container, false)

            return view
        }

    }

}
