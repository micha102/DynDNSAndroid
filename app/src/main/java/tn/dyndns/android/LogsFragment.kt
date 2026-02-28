package tn.dyndns.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LogsFragment : Fragment() {

    private lateinit var logAdapter: LogAdapter
    private lateinit var viewModel: UpdateViewModel
    private lateinit var recyclerView: RecyclerView

    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var textSize = 12f // Default text size

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_logs, container, false)
        recyclerView = view.findViewById(R.id.logRecyclerView)

        viewModel = ViewModelProvider(requireActivity())[UpdateViewModel::class.java]
        logAdapter = LogAdapter()

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = logAdapter

        // Setup scale gesture detector for pinch-to-zoom
        scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                textSize *= scaleFactor

                // Limit text size between 8sp and 24sp
                textSize = textSize.coerceIn(8f, 24f)

                // Update all visible views with new text size
                updateTextSize()
                return true
            }
        })

        // Set touch listener to handle scale gestures
        recyclerView.setOnTouchListener { _, event ->
            scaleGestureDetector?.onTouchEvent(event)
            false // Return false to allow RecyclerView to still scroll
        }

        viewModel.logs.observe(viewLifecycleOwner) { logs ->
            logAdapter.submitList(logs)
            if (logs.isNotEmpty()) recyclerView.scrollToPosition(0)
        }

        return view
    }

    private fun updateTextSize() {
        // Update all currently visible items
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val viewHolder = recyclerView.getChildViewHolder(child)
            if (viewHolder is LogAdapter.ViewHolder) {
                viewHolder.setTextSize(textSize)
            }
        }
        // Notify adapter that text size changed
        logAdapter.setTextSize(textSize)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.setOnTouchListener(null)
    }
}