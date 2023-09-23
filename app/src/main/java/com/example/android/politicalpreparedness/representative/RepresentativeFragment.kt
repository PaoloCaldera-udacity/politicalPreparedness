package com.example.android.politicalpreparedness.representative

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.android.politicalpreparedness.LocationAppServices
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.databinding.FragmentRepresentativeBinding

class RepresentativeFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: FragmentRepresentativeBinding
    private val viewModel: RepresentativeViewModel by viewModels {
        RepresentativeViewModel.RepresentativeViewModelFactory()
    }

    // LocationAppServices: class with methods for checking location permission and activation
    private val locationAppServices = LocationAppServices(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentRepresentativeBinding.inflate(inflater)
        binding.lifecycleOwner = this@RepresentativeFragment

        // When representative list is populated, make the recycler view visible
        viewModel.representativeList.observe(viewLifecycleOwner) { list ->
            list?.let {
                binding.apply {
                    listPlaceholder.visibility = View.GONE
                    representativesRecyclerView.visibility = View.VISIBLE
                }
            }
        }

        // Trigger the location permission check
        viewModel.locationPermissionFlag.observe(viewLifecycleOwner) { flag ->
            if (flag && locationAppServices.checkLocationPermission()) {
                viewModel.activeDeviceLocationFlagOn()
                viewModel.locationPermissionFlagOff()
            }
        }

        // Trigger the device location enabled check
        viewModel.activeDeviceLocationFlag.observe(viewLifecycleOwner) { flag ->
            if (flag) {
                locationAppServices.enableDeviceLocation()
                    .addOnSuccessListener {// Device location already enabled
                        viewModel.currentLocationFlagOn()
                        viewModel.activeDeviceLocationFlagOff()
                    }
                    .addOnFailureListener { exception ->  // Device location currently inactive
                        if (!locationAppServices.solveOnDeviceLocationInactive(exception))
                            // When there is no automatic resolution, reset the flag for the next check
                            viewModel.activeDeviceLocationFlagOff()
                    }
            }
        }

        // Trigger the calculation of the user's current location
        viewModel.currentLocationFlag.observe(viewLifecycleOwner) { flag ->
            if (flag) {
                locationAppServices.getCurrentLocation()
                    .addOnSuccessListener { location ->  // Location has been correctly retrieved
                        viewModel.geocodeLocationFlagOn(location)
                        viewModel.currentLocationFlagOff()
                    }
                    .addOnFailureListener { // Location has not been retrieved
                        locationAppServices.onCurrentLocationError()
                        viewModel.currentLocationFlagOff()
                    }
            }
        }

        // Trigger the current location decoding, to obtain the address
        viewModel.geocodeLocationFlag.observe(viewLifecycleOwner) { location ->
            location?.let {
                val address = locationAppServices.geocodeLocation(it)
                viewModel.geocodeLocationFlagOff(address)
            }
        }

        // Initialize the spinner by applying to it an array adapter
        val spinner = binding.state
        spinner.onItemSelectedListener = this@RepresentativeFragment
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.states,
            android.R.layout.simple_spinner_item
        ).also { arrayAdapter ->
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = arrayAdapter
        }

        return binding.root
    }


    override fun onStart() {
        super.onStart()

        // Reset all the flags when accessing the fragment or upon configuration changes
        viewModel.resetFlags()

        // Hide the recycler view if the live data variable contains an empty list
        if (viewModel.representativeList.value == null) binding.apply {
            representativesRecyclerView.visibility = View.GONE
            listPlaceholder.visibility = View.VISIBLE
        }
    }


    // Spinner selection
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        parent?.let { adapterView ->
           viewModel.state.value = adapterView.getItemAtPosition(position) as String
        }
    }

    // Spinner null selection
    override fun onNothingSelected(parent: AdapterView<*>?) {}


    /**
     * Check the result of the permission grant request
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (locationAppServices.onRequestPermissionsResult(grantResults)) {
            viewModel.activeDeviceLocationFlagOn()
        }
        /* Reset the permission flag:
           - for the next check if permissions are not granted yet
           - for finishing the location permission check if permissions are granted
         */
        viewModel.locationPermissionFlagOff()
    }

    /**
     * Result for the device location activation request
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LocationAppServices.TURN_DEVICE_LOCATION_ON_REQUEST_CODE) {
            // Turn off and on again the flag to perform automatically another check
            viewModel.activeDeviceLocationFlagOff()
            viewModel.activeDeviceLocationFlagOn()
        }
    }
}