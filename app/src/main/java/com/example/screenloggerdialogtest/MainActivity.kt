package com.example.screenloggerdialogtest

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar
import kotlin.math.min

class MainActivity : FragmentActivity() {

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

        setContentView(R.layout.activity_main)

        FirebaseUtils.authenticateUser(
            onSuccess = { uid ->
                Toast.makeText(this, "Authentication succeeded! UID: $uid", Toast.LENGTH_SHORT).show()
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, "Authentication failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

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

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // this launches the permission request
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_NOTIFICATION)
            }
        } else {
            true // For Android versions below 13, permissions are granted by default
        }
        */

        study_state = getStudyState(this)

        studyPhaseText.text = "Study phase: " + when (study_state) {
            STUDY_STATE_FIRST_LAUNCH -> "Start"
            STUDY_STATE_CONSENT_GIVEN -> "User input required before baseline"
            STUDY_STATE_BASELINE_ONGOING -> "Baseline"
            STUDY_STATE_POST_BASELINE -> "User input required before Intervention #1"
            STUDY_STATE_INT1 -> "Intervention #1"
            STUDY_STATE_POST_INT1 -> "User input required before Intervention #2"
            STUDY_STATE_INT2 -> "Intervention #2"
            STUDY_STATE_POST_INT2_SURVEY_REQUIRED -> "Post Intervention 2 - Survey responses required"
            STUDY_STATE_COMPLETE -> "Study complete"
            else -> "Unknown state"
        }

        studyPhaseSliderValue = when {
            study_state < STUDY_STATE_BASELINE_ONGOING -> 0f
            study_state < STUDY_STATE_INT1 -> 1f
            study_state < STUDY_STATE_INT2 -> 2f
            study_state <= STUDY_STATE_POST_INT2_SURVEY_REQUIRED -> 3f
            study_state <= STUDY_STATE_COMPLETE -> 4f
            else -> 0f
        }
        studyPhaseSlider.value = studyPhaseSliderValue

    }

    fun refreshUI() {
        this.recreate()
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
        private lateinit var inflaterView : View

        private lateinit var startOnboardingButton: Button
        private lateinit var allowNotificationButton: Button
        private lateinit var disableBatteryManagementButton: Button
        private lateinit var consentSpaiButton: Button
        private lateinit var consentSASButton: Button
        private lateinit var startBaselineButton: Button
        private lateinit var allowNotificationIcon: ImageView
        private lateinit var consentSpaiIcon: ImageView
        private lateinit var consentSASIcon: ImageView

        private lateinit var baselineProgressSlider: Slider

        private lateinit var averageUsageText: TextView
        private lateinit var reduceUsageSlider: Slider
        private lateinit var reduceUsageText: TextView
        private lateinit var bedTimeInput : EditText
        private lateinit var enableOverlayButton: Button
        private  lateinit var intervention1TestButton: Button
        private lateinit var startInterventionButton: Button

        private lateinit var intervention1ProgressSlider: Slider
        private lateinit var startIntervention2Button: Button
        private lateinit var intervention2ProgressSlider: Slider
        private lateinit var postStudyExitSurveyButton: Button
        private lateinit var postStudySpaiButton: Button
        private lateinit var completeStudyButton: Button

        private val POST_NOTIFICATIONS_PERMISSION : String = "android.permission.POST_NOTIFICATIONS"

        @SuppressLint("NewApi", "DefaultLocale")
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {

            val studyStateVars = getStudyStateVariables(requireContext())

            val studyState = getStudyState(requireContext())
            if (studyState == STUDY_STATE_FIRST_LAUNCH) {
                inflaterView = inflater.inflate(R.layout.first_launch_layout, container, false)

                startOnboardingButton = inflaterView.findViewById<Button>(R.id.startOnboardingButton)
                startOnboardingButton.setOnClickListener {
                    val intent = Intent(activity, OnboardingActivity::class.java)
                    startActivity(intent)
                }
            }

            else if (studyState == STUDY_STATE_CONSENT_GIVEN) {
                // Additional actions after consent is given
                // e.g., prepare the app for baseline tracking, show notification prompt
                inflaterView = inflater.inflate(R.layout.consent_given_layout, container, false)

                allowNotificationButton = inflaterView.findViewById(R.id.allowNotificationButton)
                disableBatteryManagementButton = inflaterView.findViewById(R.id.disableBatteryManagementButton)
                consentSpaiButton = inflaterView.findViewById(R.id.consentSpaiButton)
                consentSASButton = inflaterView.findViewById(R.id.consentSASButton)
                startBaselineButton = inflaterView.findViewById(R.id.startBaselineButton)
                allowNotificationIcon = inflaterView.findViewById(R.id.allowNotificationIcon)
                consentSpaiIcon = inflaterView.findViewById(R.id.spaiIcon)
                consentSASIcon = inflaterView.findViewById(R.id.sasIcon)

                var notificationsAllowed : Boolean = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            POST_NOTIFICATIONS_PERMISSION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(POST_NOTIFICATIONS_PERMISSION),
                            100
                        )
                    } else {
                        notificationsAllowed = true
                        allowNotificationIcon.isVisible = true
                        allowNotificationButton.setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.blue_gray_200
                            )
                        )
                        allowNotificationButton.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.blue_gray_600
                            )
                        )
                        checkEnableConsentButton(studyStateVars, notificationsAllowed)
                    }
                    allowNotificationButton.setOnClickListener {
                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_NOTIFICATION)
                    }
                }
                else {
                    notificationsAllowed = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
                    allowNotificationButton.setOnClickListener {
                        val intent = Intent()
                        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                        requireContext().startActivity(intent)
                    }
                }
                allowNotificationIcon.isVisible = notificationsAllowed
                allowNotificationButton.isEnabled = !notificationsAllowed

                disableBatteryManagementButton.setOnClickListener {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                    Toast.makeText(requireContext(), "Please find Android Smartphone Interventions and disable battery management. Select 'all applications' if have trouble finding it!", Toast.LENGTH_LONG).show()
                }

                consentSpaiButton.setOnClickListener {
                    val intent = Intent(activity, SPAIActivity::class.java)
                    intent.putExtra("source", "consent_given")
                    startActivity(intent)
                }

                if (studyStateVars[SPAI_1_SUBMITTED] == 1) {
                    consentSpaiIcon.visibility = View.VISIBLE
                    disableConsentButton(consentSpaiButton)
                } else {
                    consentSpaiIcon.visibility = View.GONE
                }

                consentSASButton.setOnClickListener {
                    val intent = Intent(activity, SASSVActivity::class.java)
                    intent.putExtra("source", "consent_given")
                    startActivity(intent)
                }

                if (studyStateVars[SASSV_1_SUBMITTED] == 1) {
                    consentSASIcon.visibility = View.VISIBLE
                    disableConsentButton(consentSASButton)
                } else {
                    consentSASIcon.visibility = View.GONE
                }

                startBaselineButton.setOnClickListener {
                    setStudyState(requireContext(), STUDY_STATE_BASELINE_ONGOING)
                    setStudyTimestamp(requireContext(), BASELINE_START_TIMESTAMP, System.currentTimeMillis())
                    (requireActivity() as MainActivity).refreshUI()
                }

                checkEnableConsentButton(studyStateVars, notificationsAllowed)

            }

            else if (studyState == STUDY_STATE_BASELINE_ONGOING) {
                // Actions for baseline ongoing
                // e.g., initialize baseline tracking, display tracking progress
                inflaterView = inflater.inflate(R.layout.baseline_ongoing_layout, container, false)

                val baselineDay = calculateStudyPeriodDay(BASELINE_START_TIMESTAMP, requireContext())
                if (baselineDay > BASELINE_DURATION) {
                    setStudyState(requireContext(), STUDY_STATE_POST_BASELINE)
                    (requireActivity() as MainActivity).refreshUI()
                }

                checkAndStartBaselineService()

                FirebaseUtils.fetchUsageTotal(LocalDateTime.now()) { usage ->
                    Log.d("FIREBASE", "Total Usage All Days: $usage")
                }

                baselineProgressSlider = inflaterView.findViewById(R.id.baselineProgressSlider)
                baselineProgressSlider.value = min(baselineDay.toFloat(), BASELINE_DURATION.toFloat())
            }

            else if (studyState == STUDY_STATE_POST_BASELINE) {
                checkAndStartBaselineService()

                var usageAverage = 0L
                // Actions for post-baseline phase
                // e.g., gather baseline data, prepare for intervention
                inflaterView = inflater.inflate(R.layout.post_baseline_layout, container, false)

                averageUsageText = inflaterView.findViewById(R.id.averageUsageText)
                reduceUsageSlider = inflaterView.findViewById(R.id.settingsReduceUsageSlider)
                reduceUsageText = inflaterView.findViewById(R.id.reduceUsageText)
                bedTimeInput = inflaterView.findViewById(R.id.bedtimeInput)
                enableOverlayButton = inflaterView.findViewById(R.id.enableOverlayButton)
                intervention1TestButton = inflaterView.findViewById(R.id.intervention1TestButton)
                startInterventionButton = inflaterView.findViewById(R.id.startInterventionButton)

                FirebaseUtils.fetchUsageTotal(LocalDateTime.now()) { usage ->
                    val totalUsageAllDays = usage.values.sum()
                    val averageUsageMillis = if (usage.isNotEmpty()) totalUsageAllDays / usage.size else 0L

                    val hours = averageUsageMillis / (1000 * 60 * 60)
                    val minutes = (averageUsageMillis / (1000 * 60)) % 60

                    val usageFormatted = String.format("%02d:%02d", hours, minutes)
                    val usageText = String.format(
                        averageUsageText.context.getString(R.string.daily_usage),
                        usageFormatted
                    )
                    averageUsageText.text = usageText
                    usageAverage = averageUsageMillis
                }

                reduceUsageSlider.setLabelFormatter {
                    value -> "-${value.toInt()}%"
                }

                var reduceUsageTouched = false
                var reducedUsageMillis = 0L
                reduceUsageSlider.addOnChangeListener { slider, value, _ ->
                    // Convert the slider value to a percentage (negative)
                    val percentageReduction = -value.toInt()

                    // Calculate the reduced usage in milliseconds
                    reducedUsageMillis = usageAverage * (100 + percentageReduction) / 100

                    // Convert reduced usage to HH:mm format
                    val hours = reducedUsageMillis / (1000 * 60 * 60)
                    val minutes = (reducedUsageMillis / (1000 * 60)) % 60
                    val reducedUsageFormatted = String.format("%02dh %02dm", hours, minutes)

                    // Update the TextView with the reduced usage
                    reduceUsageText.text = String.format(
                        reduceUsageText.context.getString(R.string.usage_limit_text),
                        reducedUsageFormatted
                    )
                    reduceUsageTouched = true
                }
                reduceUsageText.text = "Move the slider to select between 10% and 50% goal in reduction of smartphone use"

                bedTimeInput.setOnClickListener {
                    val calendar = Calendar.getInstance()
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(Calendar.MINUTE)

                    val timePicker = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                        bedTimeInput.setText(
                            String.format(
                                "%02dh %02dm",
                                selectedHour,
                                selectedMinute
                            )
                        )
                    }, hour, minute, true)
                    timePicker.show()
                }

                enableOverlayButton = inflaterView.findViewById(R.id.enableOverlayButton)
                enableOverlayButton.setOnClickListener {
                    (requireActivity() as MainActivity).requestOverlayPermission()
                }

                var overlaysAllowed = false
                if (Settings.canDrawOverlays(requireContext())) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        overlaysAllowed = true
                        // button disabled if already enabled
                        disableINT1Button(enableOverlayButton)
                    }
                    else {
                        Log.d("MAIN:ONCREATE", "SDK level too low")
                    }
                }

                var interventionTested = false
                intervention1TestButton.setOnClickListener {
                    UnlockDialog().showDialog(requireContext(), 0,0,0,0, DIALOG_TYPE_GOAL_EXCEEDED, true)
                    //
                    interventionTested = true
                }
                intervention1TestButton.isEnabled = overlaysAllowed

                startInterventionButton.setOnClickListener {
                    // TODO send setup data to firebase
                    // TODO and store to local vars
                    Log.d("MAIN", bedTimeInput.text.toString())
                    if (overlaysAllowed && bedTimeInput.text.isNotEmpty() && reduceUsageTouched && interventionTested) {
                        val c = requireContext()
                        setStudyState(c, STUDY_STATE_INT1)
                        setStudyVariable(c, BASELINE_USAGE_AVERAGE, usageAverage)
                        setStudyVariable(c, INT_SMARTPHONE_USAGE_LIMIT_GOAL, reducedUsageMillis)
                        setStudyVariable(c, INT_SMARTPHONE_USAGE_LIMIT_PERCENTAGE, reduceUsageSlider.value.toInt())
                        setStudyVariable(c, BEDTIME_GOAL, parseBedtimeToMinutes(bedTimeInput.text.toString()))
                        setStudyTimestamp(c, INT1_START_TIMESTAMP, System.currentTimeMillis())

                        val goalData = hashMapOf(
                            INT1_START_TIMESTAMP to System.currentTimeMillis(),
                            INT_SMARTPHONE_USAGE_LIMIT_GOAL to reducedUsageMillis,
                            BEDTIME_GOAL to parseBedtimeToMinutes(bedTimeInput.text.toString()),
                            "ts" to System.currentTimeMillis()
                        )

                        FirebaseUtils.sendEntryToDatabase(
                            path = "users/${FirebaseUtils.getCurrentUserUID()}/study_state_info",
                            data = goalData,
                            onSuccess = {
                                //Toast.makeText(requireContext(), "Screen event sent successfully", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { exception ->
                                // Handle failure
                                //Toast.makeText(requireContext(), "Failed to send data: ${exception.message}", Toast.LENGTH_LONG).show()
                            }
                        )

                        (requireActivity() as MainActivity).refreshUI()
                    }
                    // prompt user to do the missing thing!
                    else {
                        if (!overlaysAllowed) {
                            Toast.makeText(requireContext(), "Please allow overlays before starting Intervention Phase #1", Toast.LENGTH_SHORT).show()
                        }
                        else if (bedTimeInput.text.isEmpty()) {
                            Toast.makeText(requireContext(), "Please give your preferred bedtime before starting Intervention Phase #1", Toast.LENGTH_SHORT).show()
                        }
                        else if (!reduceUsageTouched) {
                            Toast.makeText(requireContext(), "Please set a smartphone usage goal before starting Intervention Phase #1", Toast.LENGTH_SHORT).show()
                        }
                        else if (!interventionTested) {
                            Toast.makeText(requireContext(), "Please test the intervention screen before starting Intervention Phase #1", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            }

            else if (studyState == STUDY_STATE_INT1) {
                // e.g., initiate intervention routines, set up reminders
                inflaterView = inflater.inflate(R.layout.intervention1_layout, container, false)

                // INT1 requires overlay permission
                if (Settings.canDrawOverlays(requireContext())) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!isServiceRunning(requireContext(), INT1Service::class.java)) {
                            val intent = Intent(requireContext(), INT1Service::class.java) // Build the intent for the service
                            (requireActivity() as MainActivity).startForegroundService(intent)
                        }
                    }
                    else {
                        Log.d("MAIN:ONCREATE", "SDK level too low")
                    }
                } else {
                    // this prompts user to the settings screen to enable overlay
                    Toast.makeText(requireContext(), "Please enable overlay permission", Toast.LENGTH_LONG).show()
                    (requireActivity() as MainActivity).requestOverlayPermission()
                }

                val int1Day = calculateStudyPeriodDay(INT1_START_TIMESTAMP, requireContext())
                if (int1Day > INT1_DURATION) {
                    setStudyState(requireContext(), STUDY_STATE_POST_INT1)
                    (requireActivity() as MainActivity).refreshUI()
                }

                checkAndStartBaselineService()

                intervention1ProgressSlider = inflaterView.findViewById(R.id.intervention1ProgressSlider)
                intervention1ProgressSlider.value = min(int1Day.toFloat(), INT1_DURATION.toFloat())

            }

            else if (studyState == STUDY_STATE_POST_INT1) {
                // Actions after Intervention Phase 1
                // e.g., prompt user feedback on phase, analyze results
                inflaterView = inflater.inflate(R.layout.post_int1_layout, container, false)

                startIntervention2Button = inflaterView.findViewById(R.id.startIntervention2Button)
                startIntervention2Button.setOnClickListener {
                    setStudyState(requireContext(), STUDY_STATE_INT2)
                    setStudyTimestamp(requireContext(), INT2_START_TIMESTAMP, System.currentTimeMillis())

                    val goalData = hashMapOf(
                        INT2_START_TIMESTAMP to System.currentTimeMillis(),
                        "ts" to System.currentTimeMillis()
                    )

                    FirebaseUtils.sendEntryToDatabase(
                        path = "users/${FirebaseUtils.getCurrentUserUID()}/study_state_info",
                        data = goalData,
                        onSuccess = {
                            //Toast.makeText(requireContext(), "Screen event sent successfully", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { exception ->
                            // Handle failure
                            //Toast.makeText(requireContext(), "Failed to send data: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                    )

                    checkAndStartBaselineService()

                    (requireActivity() as MainActivity).refreshUI()
                }
            }

            else if (studyState == STUDY_STATE_INT2) {
                // Actions for Intervention Phase 2
                // e.g., adjust intervention parameters, set up next steps
                inflaterView = inflater.inflate(R.layout.intervention2_layout, container, false)

                val int2Day = calculateStudyPeriodDay(INT2_START_TIMESTAMP, requireContext())
                if (int2Day > INT2_DURATION) {
                    setStudyState(requireContext(), STUDY_STATE_POST_INT2_SURVEY_REQUIRED)
                    (requireActivity() as MainActivity).refreshUI()
                }

                checkAndStartBaselineService()

                if (!isServiceRunning(requireContext(), INT2Service::class.java)) {
                    val intent = Intent(
                        requireContext(),
                        INT2Service::class.java
                    ) // Build the intent for the service
                    (requireActivity() as MainActivity).startForegroundService(intent)
                }

                intervention2ProgressSlider = inflaterView.findViewById(R.id.intervention2ProgressSlider)

                val safeInt2Day = if (int2Day < 1) 1 else int2Day
                intervention2ProgressSlider.value = min(safeInt2Day.toFloat(), INT2_DURATION.toFloat())

            }

            else if (studyState == STUDY_STATE_POST_INT2_SURVEY_REQUIRED) {
                // Actions for post Intervention Phase 2 survey
                // e.g., collect survey data, prepare for final analysis
                inflaterView = inflater.inflate(R.layout.post_int2_survey_required_layout, container, false)
            }

            else if (studyState == STUDY_STATE_COMPLETE) {
                // Actions for study completion
                // e.g., show summary, provide exit message, share results
                inflaterView = inflater.inflate(R.layout.complete_layout, container, false)
            } else {
                // Optional fallback layout and actions
                inflaterView = inflater.inflate(R.layout.fragment_info, container, false)
            }

            return inflaterView
        }

        private fun checkAndStartBaselineService() {
            if (!isServiceRunning(requireContext(), BaselineService::class.java)) {
                val intent = Intent(
                    requireContext(),
                    BaselineService::class.java
                ) // Build the intent for the service
                (requireActivity() as MainActivity).startForegroundService(intent)
            }
        }

        private fun checkEnableConsentButton(studyStateVars : Map<String, Number?>, notificationsAllowed : Boolean) {
            if (!((studyStateVars[SASSV_1_SUBMITTED] == 1) && (studyStateVars[SPAI_1_SUBMITTED] ==1) && notificationsAllowed)) {
                disableConsentButton(startBaselineButton)
            }
            else {
                enableConsentButton(startBaselineButton)
            }
        }

        private fun disableConsentButton(b : Button) {
            b.isEnabled = false
            b.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_gray_200))
            b.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_gray_600))
        }

        private fun enableConsentButton(b : Button) {
            b.isEnabled = true
            b.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_gray_700))
            b.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_gray_50))
        }

        private fun disableINT1Button(b : Button) {
            b.isEnabled = false
            b.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_200))
            b.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_600))
        }

    }

    class DataCollectedFragment : Fragment() {
        private lateinit var inflaterView : View

        private lateinit var usageBarChart : BarChart

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {

            val baselineDay = calculateStudyPeriodDay(BASELINE_START_TIMESTAMP, requireContext())

            if (baselineDay < 2) {
                inflaterView = inflater.inflate(R.layout.fragment_data_collected_empty, container, false)
                return inflaterView
            }
            else {
                inflaterView = inflater.inflate(R.layout.fragment_data_collected, container, false)

                usageBarChart = inflaterView.findViewById(R.id.dailyUsageBarChart)

                FirebaseUtils.fetchUsageTotal(LocalDateTime.now()) { usage ->
                    val entries = mutableListOf<BarEntry>()
                    val today = LocalDate.now()
                    val startDate = today.minusDays(1)  // Yesterday
                    val endDate = today.minusWeeks(2)    // Two weeks behind
                    val dates =
                        mutableListOf<Pair<LocalDate, Float>>()  // To store dates and corresponding usage in minutes

                    var currentDate = startDate
                    while (!currentDate.isBefore(endDate)) {
                        val usageInMillis = usage[currentDate.toString()]

                        // If there is usage data for this date, create a BarEntry
                        if (usageInMillis != null) {
                            Log.d("DATE_DEBUG_DUMP", "Usage on $currentDate: $usageInMillis")
                            // Convert the usage from milliseconds to minutes
                            val minutes = (usageInMillis / 60000).toFloat() // Convert to minutes
                            val p = Pair(currentDate, minutes)
                            dates.add(p)
                        } else {
                            dates.add(Pair(currentDate, 0F))
                            //Log.d("DATE_DEBUG_DUMP", "No usage found for $currentDate")
                        }
                        // Move to the next date
                        currentDate = currentDate.minusDays(1)
                    }
                    val sortedDates = dates.sortedBy { it.first }  // Sorting by the LocalDate

                    // Create BarEntry from sorted data
                    for (entry in sortedDates) {
                        val date = entry.first
                        val minutes = entry.second
                        val barEntry = BarEntry(date.dayOfMonth.toFloat(), minutes)
                        entries.add(barEntry)
                    }

                    // Create a BarDataSet
                    val barDataSet = BarDataSet(entries, "")
                    barDataSet.color = resources.getColor(R.color.deep_purple_500, null)

                    // Set up BarData
                    val barData = BarData(barDataSet)
                    barData.barWidth = 0.8f // Set bar width

                    // Configure the chart
                    usageBarChart.data = barData
                    usageBarChart.description.text =
                        "Smartphone usage data from past two weeks (in minutes)"
                    usageBarChart.description.textSize = 14f // Optional: Adjust text size
                    usageBarChart.description.setPosition(
                        usageBarChart.width / 2f,
                        30f
                    ) // Adjust X and Y position (pixels)
                    usageBarChart.description.textAlign =
                        Paint.Align.CENTER // Align the text to the center
                    usageBarChart.setFitBars(true) // Make the bars fit into the chart view

                    // Customize the X-Axis
                    val xAxis: XAxis = usageBarChart.xAxis
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.granularity = 1f // One interval per bar
                    xAxis.setDrawGridLines(false)

                    // Customize the Y-Axis (optional)
                    val yAxis = usageBarChart.axisLeft
                    yAxis.axisMinimum = 0f // Optional: Set minimum value for the axis
                    yAxis.granularity = 1f // Optional: Set the interval between labels
                    yAxis.textSize = 12f // Optional: Adjust text size

                    val yAxisRight: YAxis = usageBarChart.axisRight
                    yAxisRight.isEnabled = false // Disable right Y-Axis

                    // Refresh the chart
                    usageBarChart.invalidate() // Refresh chart with new data
                }

                return inflaterView
            }
        }
    }

    // settings fragment is always the same
    // some fields are disabled field.isEnabled = false prior to INT1
    class SettingsFragment : Fragment() {
        private lateinit var studyStateSpinner: Spinner
        private lateinit var settingsReduceUsageSlider: Slider
        private lateinit var settingsReduceUsageText : TextView
        private lateinit var settingsLayout : LinearLayout
        private lateinit var passkeyEditText : EditText
        private lateinit var passkeyText : TextView
        private lateinit var settingsGoalLayout : LinearLayout
        private lateinit var settingsDeviceidText : TextView

        private lateinit var testDialogButtonGoal : Button
        private lateinit var testDialogButtonBed : Button

        private lateinit var clearSharedPreferencesButton : Button
        private lateinit var clearDailyUsageButton : Button

        private var isUserInteracting = false
        private var usageAverage = 0L

        @SuppressLint("StringFormatInvalid", "DefaultLocale")
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            // Inflate the layout for this fragment
            val view = inflater.inflate(R.layout.fragment_settings, container, false)

            settingsReduceUsageSlider = view.findViewById(R.id.settingsReduceUsageSlider)
            settingsReduceUsageText = view.findViewById(R.id.settingsReduceUsageText)
            settingsLayout = view.findViewById(R.id.settingsHiddenLayout)
            passkeyEditText = view.findViewById<EditText>(R.id.passkeyEditText)
            passkeyText = view.findViewById(R.id.passkeyText)
            settingsGoalLayout = view.findViewById(R.id.settingsGoalLayout)
            testDialogButtonGoal = view.findViewById(R.id.settingsTestDialogGoal)
            testDialogButtonBed = view.findViewById(R.id.settingsTestDialogBed)
            settingsDeviceidText = view.findViewById(R.id.settingsDeviceId)
            clearSharedPreferencesButton = view.findViewById(R.id.clearSharedPreferences)
            clearDailyUsageButton = view.findViewById(R.id.clearDailyUsage)

            usageAverage = getStudyVariable(requireContext(), BASELINE_USAGE_AVERAGE, 0L)

            settingsReduceUsageSlider.setLabelFormatter {
                    value -> "-${value.toInt()}%"
            }

            var reducedUsageMillis = 0L
            settingsReduceUsageSlider.addOnChangeListener { _, value, _ ->
                // Convert the slider value to a percentage (negative)
                val percentageReduction = -value.toInt()

                // Calculate the reduced usage in milliseconds
                reducedUsageMillis = usageAverage * (100 + percentageReduction) / 100

                // Convert reduced usage to HH:mm format
                val hours = reducedUsageMillis / (1000 * 60 * 60)
                val minutes = (reducedUsageMillis / (1000 * 60)) % 60
                val reducedUsageFormatted = String.format("%02dh %02dm", hours, minutes)

                settingsReduceUsageText.text = String.format(
                    settingsReduceUsageText.context.getString(R.string.usage_limit_text),
                    reducedUsageFormatted
                )

                // update values here automatically
                setStudyVariable(requireContext(), INT_SMARTPHONE_USAGE_LIMIT_GOAL, reducedUsageMillis)
                setStudyVariable(requireContext(), INT_SMARTPHONE_USAGE_LIMIT_PERCENTAGE, value.toInt())
            }

            val usageGoal = getStudyVariable(requireContext(), INT_SMARTPHONE_USAGE_LIMIT_PERCENTAGE, 10)
            settingsReduceUsageSlider.value = usageGoal.toFloat()
            settingsReduceUsageSlider.setLabelFormatter {
                    value -> "-${value.toInt()}%"
            }

            settingsDeviceidText.text = String.format(
                settingsDeviceidText.context.getString(R.string.settings_deviceid_text),
                FirebaseUtils.getCurrentUserUID().toString()
            )

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

            val sasButton : Button = view.findViewById(R.id.SAS_SV)
            sasButton.setOnClickListener {
                // Start the MainActivity for onboarding
                val intent = Intent(activity, SASSVActivity::class.java)
                startActivity(intent)
            }

            studyStateSpinner = view.findViewById<Spinner>(R.id.study_state_spinner)

            // Array of study states with descriptions
            val studyStates = arrayOf(
                "0 - Onboarding",
                "1 - Pre-baseline",
                "2 - Baseline",
                "3 - Pre-INT1",
                "4 - INT1",
                "5 - Post-INT1",
                "6 - INT2",
                "7 - Post-INT2",
                "8 - Study Complete"
            )

            // Set up the ArrayAdapter
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, studyStates)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            studyStateSpinner.adapter = adapter

            studyStateSpinner.setOnTouchListener { _, _ ->
                isUserInteracting = true
                false // Allow the touch event to propagate
            }
            studyStateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    if (isUserInteracting) {
                        val selectedState = position // Store the selected index (0-8)
                        // Optional: Show a Toast message for demonstration
                        setStudyState(requireContext(), selectedState)
                        (requireActivity() as MainActivity).refreshUI()
                        // Reset the interaction flag
                        isUserInteracting = false
                    }

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing, or handle if needed
                }
            }
            studyStateSpinner.setSelection(getStudyState(requireContext()))

            testDialogButtonGoal.setOnClickListener {
                UnlockDialog().showDialog(
                    requireContext(),
                    0,
                    0,
                    0,
                    0,
                    DIALOG_TYPE_GOAL_EXCEEDED,
                    true
                )
            }

            testDialogButtonBed.setOnClickListener {
                UnlockDialog().showDialog(
                    requireContext(),
                    0,
                    0,
                    0,
                    0,
                    DIALOG_TYPE_BEDTIME,
                    true
                )
            }

            clearSharedPreferencesButton.setOnClickListener {
                showClearConfirmationDialog(requireContext())
            }

            clearDailyUsageButton.setOnClickListener {
                resetDailyUsage(requireContext())
            }

            // Set settings content to hidden unless passkey is entered
            settingsLayout.visibility = View.GONE

            passkeyEditText.setText("")
            passkeyEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s.toString() == SETTINGS_PASSKEY) {
                        // Passkey is correct
                        settingsLayout.visibility = View.VISIBLE
                        passkeyText.text = "Passkey correct!"
                    }
                    else {
                        settingsLayout.visibility = View.GONE
                        passkeyText.text = "Enter passkey:"
                    }
                }
            })

            if (getStudyState(requireContext()) < 3) {
                settingsGoalLayout.visibility = View.GONE
            }

            return view
        }
    }
}
