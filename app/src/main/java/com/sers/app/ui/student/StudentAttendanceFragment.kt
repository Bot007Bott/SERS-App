package com.sers.app.ui.student

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sers.app.R
import com.sers.app.databinding.FragmentStudentAttendanceBinding
import com.sers.app.model.Attendance
import com.sers.app.model.Course
import com.sers.app.ui.admin.FilterOptionAdapter
import com.sers.app.viewmodel.StudentAttendanceViewModel

/**
 * StudentAttendanceFragment — MVVM View
 * Observes StudentAttendanceViewModel for attendance and courses data.
 */
class StudentAttendanceFragment : Fragment() {

    private lateinit var binding: FragmentStudentAttendanceBinding
    private lateinit var adapter: StudentAttendanceAdapter
    private val viewModel: StudentAttendanceViewModel by viewModels()

    private val attendanceList = mutableListOf<Attendance>()
    private val courseList = mutableListOf<Course>()
    private var currentFilterCourseId = ""
    private var currentFilterStatus = ""
    private var currentSortOrder = "Default"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStudentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StudentAttendanceAdapter(mutableListOf(), mutableListOf())
        binding.rvAttendance.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttendance.adapter = adapter

        // Observe ViewModel
        viewModel.attendance.observe(viewLifecycleOwner) { list ->
            attendanceList.clear()
            attendanceList.addAll(list)
            refreshList()
            updateSummaryCards()
        }

        viewModel.courses.observe(viewLifecycleOwner) { courses ->
            courseList.clear()
            courseList.addAll(courses)
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg.startsWith("ERROR:")) Snackbar.make(binding.root, msg.removePrefix("ERROR:"), Snackbar.LENGTH_SHORT).show()
        }

        viewModel.loadData()

        binding.btnSearch.setOnClickListener {
            if (binding.searchLayout.visibility == View.GONE) {
                binding.searchLayout.visibility = View.VISIBLE
                binding.btnSearch.setBackgroundColor(android.graphics.Color.parseColor("#1976D2"))
                binding.btnSearch.setTextColor(android.graphics.Color.WHITE)
                binding.btnSearch.setIconResource(R.drawable.ic_close)
                binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            } else {
                binding.searchLayout.visibility = View.GONE
                binding.etSearch.setText("")
                binding.btnSearch.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                binding.btnSearch.setTextColor(android.graphics.Color.parseColor("#1976D2"))
                binding.btnSearch.setIconResource(R.drawable.ic_search)
                binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2"))
                refreshList()
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val filtered = getFilteredSortedList().filter { att ->
                    val course = courseList.find { it.courseId == att.courseId }
                    course?.courseName?.contains(query, ignoreCase = true) == true
                }.toMutableList()
                adapter.updateList(filtered, courseList)
                updateEmptyState(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnFilter.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filter by")
                .setItems(arrayOf("Course", "Status")) { _, which ->
                    when (which) { 0 -> showCourseFilterSheet(); 1 -> showStatusFilterSheet() }
                }.show()
        }

        binding.btnSort.setOnClickListener {
            val sortOptions = arrayOf("Default", "Date Newest", "Date Oldest", "Course A-Z")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, sortOptions.indexOf(currentSortOrder)) { dialog, which ->
                    currentSortOrder = sortOptions[which]; refreshList(); updateSortButton(); dialog.dismiss()
                }.show()
        }
    }

    private fun refreshList() {
        val list = getFilteredSortedList()
        adapter.updateList(list, courseList)
        updateEmptyState(list)
    }

    private fun getFilteredSortedList(): MutableList<Attendance> {
        var list = attendanceList.toMutableList()
        if (currentFilterCourseId.isNotEmpty()) list = list.filter { it.courseId == currentFilterCourseId }.toMutableList()
        if (currentFilterStatus.isNotEmpty()) list = list.filter { it.status == currentFilterStatus }.toMutableList()
        return when (currentSortOrder) {
            "Date Newest" -> list.sortedByDescending { it.date }.toMutableList()
            "Date Oldest" -> list.sortedBy { it.date }.toMutableList()
            "Course A-Z" -> list.sortedBy { att -> courseList.find { it.courseId == att.courseId }?.courseName }.toMutableList()
            else -> list
        }
    }

    private fun showCourseFilterSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView)
        sheetView.findViewById<android.widget.TextView>(R.id.tvFilterTitle).text = "Filter by Course"
        val allOptions = listOf(Pair("All Courses", "")) + courseList.map { Pair(it.courseName, it.courseId) }
        var filteredOptions = allOptions.toMutableList()
        val rv = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val filterAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            inner class VH(val b: com.sers.app.databinding.ItemFilterOptionBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val b = com.sers.app.databinding.ItemFilterOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false); return VH(b)
            }
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val option = filteredOptions[position]; (holder as VH).b.tvOption.text = option.first
                holder.b.ivCheck.visibility = if (option.second == currentFilterCourseId) View.VISIBLE else View.GONE
                holder.b.root.setOnClickListener { currentFilterCourseId = option.second; refreshList(); updateFilterButton(); bottomSheet.dismiss() }
            }
            override fun getItemCount() = filteredOptions.size
            fun updateList(newList: MutableList<Pair<String, String>>) { filteredOptions = newList; notifyDataSetChanged() }
        }
        rv.adapter = filterAdapter
        sheetView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFilterSearch)
            .addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val q = s.toString().lowercase()
                    filterAdapter.updateList(if (q.isEmpty()) allOptions.toMutableList() else allOptions.filter { it.first.lowercase().contains(q) }.toMutableList())
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        bottomSheet.show()
    }

    private fun showStatusFilterSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(sheetView)
        sheetView.findViewById<android.widget.TextView>(R.id.tvFilterTitle).text = "Filter by Status"
        val allOptions = mutableListOf("All", "Present", "Absent", "Late")
        val rv = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFilterOptions)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val filterAdapter = FilterOptionAdapter(allOptions, if (currentFilterStatus.isEmpty()) "All" else currentFilterStatus) { selected ->
            currentFilterStatus = if (selected == "All") "" else selected; refreshList(); updateFilterButton(); bottomSheet.dismiss()
        }
        rv.adapter = filterAdapter
        bottomSheet.show()
    }

    private fun updateFilterButton() {
        val isActive = currentFilterCourseId.isNotEmpty() || currentFilterStatus.isNotEmpty()
        binding.btnFilter.text = if (!isActive) "Filter" else "Filter •"
        binding.btnFilter.setBackgroundColor(if (!isActive) android.graphics.Color.TRANSPARENT else android.graphics.Color.parseColor("#1976D2"))
        binding.btnFilter.setTextColor(if (!isActive) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.WHITE)
        binding.btnFilter.iconTint = android.content.res.ColorStateList.valueOf(if (!isActive) android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.WHITE)
    }

    private fun updateSortButton() {
        binding.btnSort.text = if (currentSortOrder == "Default") "Sort" else "Sort •"
        binding.btnSort.setBackgroundColor(if (currentSortOrder == "Default") android.graphics.Color.TRANSPARENT else android.graphics.Color.parseColor("#1976D2"))
        binding.btnSort.setTextColor(if (currentSortOrder == "Default") android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.WHITE)
        binding.btnSort.iconTint = android.content.res.ColorStateList.valueOf(if (currentSortOrder == "Default") android.graphics.Color.parseColor("#1976D2") else android.graphics.Color.WHITE)
    }

    private fun updateSummaryCards() {
        binding.tvPresent.text = attendanceList.count { it.status == "Present" }.toString()
        binding.tvAbsent.text = attendanceList.count { it.status == "Absent" }.toString()
        binding.tvLate.text = attendanceList.count { it.status == "Late" }.toString()
    }

    private fun updateEmptyState(list: List<Any>) {
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvAttendance.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}