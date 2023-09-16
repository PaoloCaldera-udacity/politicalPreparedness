package com.example.android.politicalpreparedness.representative

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.android.politicalpreparedness.BuildConfig
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.databinding.FragmentRepresentativeBinding
import com.example.android.politicalpreparedness.network.models.Address
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class RepresentativeFragment : Fragment() {

    private lateinit var binding: FragmentRepresentativeBinding
    private val viewModel: RepresentativeViewModel by viewModels {
        RepresentativeViewModel.RepresentativeViewModelFactory()
    }

    //TODO: Declare ViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentRepresentativeBinding.inflate(inflater)
        binding.lifecycleOwner = this@RepresentativeFragment

        viewModel.representativesFlag.observe(viewLifecycleOwner) {
            if (it) binding.apply {
                listPlaceholder.visibility = View.GONE
                representativesTitle.visibility = View.VISIBLE
                representativesRecyclerView.visibility = View.VISIBLE
            } else binding.apply {
                representativesTitle.visibility = View.GONE
                representativesRecyclerView.visibility = View.GONE
                listPlaceholder.visibility = View.VISIBLE
            }
        }

        viewModel.locationPermissionFlag.observe(viewLifecycleOwner) {
            if (it) checkLocationPermission()
        }
        viewModel.activeDeviceLocationFlag.observe(viewLifecycleOwner) {
            if (it) enableDeviceLocation()
        }

        //TODO: Establish bindings

        //TODO: Define and assign Representative adapter

        //TODO: Populate Representative adapter

        //TODO: Establish button listeners for field and location search

        return binding.root
    }


    /**
     * Proceed to find location if permission is granted
     * Request instead the permission if it is not already granted
     */
    private fun checkLocationPermission(): Boolean {
        return if (isPermissionGranted()) {
            viewModel.locationPermissionFlagOff()
            viewModel.activeDeviceLocationFlagOn()
            true
        } else {
            var permissionArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            val resultCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permissionArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE
                } else FOREGROUND_PERMISSION_REQUEST_CODE
            requestPermissions(permissionArray, resultCode)
            false
        }
    }

    /**
     * Check if foreground and background permission are granted by the application
     */
    private fun isPermissionGranted(): Boolean {
        val foregroundPermissionGranted = (PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ))
        val backgroundPermissionGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else true

        return (foregroundPermissionGranted && backgroundPermissionGranted)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //TODO: Handle location permission result to get location on permission granted
        var outcome = true
        for (grant in grantResults) {
            if (grant == PackageManager.PERMISSION_DENIED) {
                outcome = false
                break
            }
        }

        if (!outcome) {
            // Build a material alert dialog that addresses the user to the settings app
            val materialAlertDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.alert_dialog_title))
                .setMessage(resources.getString(R.string.alert_dialog_message))
                .setIcon(R.drawable.ic_location)
                .setNegativeButton(resources.getString(R.string.permission_negative_button)) { dialog, _ ->
                    dialog.dismiss()
                    // Reset the permission flag for the next check
                    viewModel.locationPermissionFlagOff()
                }
                .setPositiveButton(resources.getString(R.string.permission_positive_button)) { dialog, _ ->
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    dialog.dismiss()
                    // Reset the permission flag for the next check
                    viewModel.locationPermissionFlagOff()
                }
                .create()
            materialAlertDialog.show()
        } else
            checkLocationPermission()
    }

    private fun enableDeviceLocation() {
        // Build location settings request
        val locationSettingsRequest = LocationSettingsRequest.Builder().addLocationRequest(
            LocationRequest.create().apply { priority = LocationRequest.PRIORITY_LOW_POWER }
        ).build()

        // Define settings client for location services
        val settingsClient = LocationServices.getSettingsClient(requireContext())

        // Check if the location settings are enabled
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(locationSettingsRequest)

        locationSettingsResponseTask.addOnSuccessListener {
            viewModel.activeDeviceLocationFlagOff()
            
        }
    }

    private fun getLocation() {
        // Build location settings request
        val locationSettingsRequest = LocationSettingsRequest.Builder().addLocationRequest(
            LocationRequest.create().apply { priority = LocationRequest.PRIORITY_LOW_POWER }
        ).build()
        // Define settings client for location services
        val settingsClient = LocationServices.getSettingsClient(requireContext())

        // Check if the location settings are enabled
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(locationSettingsRequest)

        locationSettingsResponseTask.addOnFailureListener {
            if (it is ResolvableApiException) {
                try {
                    startIntentSenderForResult(
                        it.resolution.intentSender,
                        TURN_DEVICE_LOCATION_ON_REQUEST_CODE,
                        null, 0, 0, 0, null
                    )
                } catch (sendException: IntentSender.SendIntentException) {
                    Toast.makeText(
                        requireContext(),
                        resources.getString(R.string.toast_error_message, sendException.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {

            }
        }
        locationSettingsResponseTask.addOnSuccessListener {

        }

        //TODO: The geoCodeLocation method is a helper function to change the lat/long location to a human readable street address
    }

    private fun geoCodeLocation(location: Location): Address {
        val geocoder = Geocoder(context, Locale.getDefault())
        return geocoder.getFromLocation(location.latitude, location.longitude, 1)
            .map { address ->
                Address(
                    address.thoroughfare,
                    address.subThoroughfare,
                    address.locality,
                    address.adminArea,
                    address.postalCode
                )
            }
            .first()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TURN_DEVICE_LOCATION_ON_REQUEST_CODE)
            getLocation()
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view!!.windowToken, 0)
    }


    companion object {
        private const val FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE = 201
        private const val FOREGROUND_PERMISSION_REQUEST_CODE = 202
        private const val TURN_DEVICE_LOCATION_ON_REQUEST_CODE = 203
    }
}