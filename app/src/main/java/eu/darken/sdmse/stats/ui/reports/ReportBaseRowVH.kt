package eu.darken.sdmse.stats.ui.reports

import android.content.res.ColorStateList
import android.text.format.DateUtils
import android.view.ViewGroup
import eu.darken.sdmse.R
import eu.darken.sdmse.common.lists.binding
import eu.darken.sdmse.databinding.StatsReportsBaseItemBinding
import eu.darken.sdmse.main.core.iconRes
import eu.darken.sdmse.main.core.labelRes
import eu.darken.sdmse.stats.core.Report
import java.time.Instant


class ReportBaseRowVH(parent: ViewGroup) :
    ReportsAdapter.BaseVH<ReportBaseRowVH.Item, StatsReportsBaseItemBinding>(
        R.layout.stats_reports_base_item,
        parent
    ) {

    override val viewBinding = lazy { StatsReportsBaseItemBinding.bind(itemView) }

    override val onBindData: StatsReportsBaseItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = binding { item ->
        val report = item.report
        toolIcon.setImageResource(report.tool.iconRes)
        toolLabel.text = getString(report.tool.labelRes)
        lastExecution.apply {
            text = DateUtils.getRelativeTimeSpanString(
                report.endAt.toEpochMilli(),
                Instant.now().toEpochMilli(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        }

        when (report.status) {
            Report.Status.SUCCESS -> {
                executionState.apply {
                    setImageResource(R.drawable.ic_check_circle)
                    imageTintList = ColorStateList.valueOf(
                        getColorForAttr(androidx.appcompat.R.attr.colorPrimary)
                    )
                }
                infoPrimary.text = report.primaryMessage
                infoSecondary.text = report.secondaryMessage
            }

            Report.Status.PARTIAL_SUCCESS -> {
                executionState.apply {
                    setImageResource(R.drawable.ic_circle_outline_24)
                    imageTintList = ColorStateList.valueOf(
                        getColorForAttr(com.google.android.material.R.attr.colorSecondary)
                    )
                }
                infoPrimary.setText(R.string.stats_report_status_partial_success)
                infoSecondary.text = report.primaryMessage
            }

            Report.Status.FAILURE -> {
                executionState.apply {
                    setImageResource(R.drawable.ic_alert_octagon_outline_24)
                    imageTintList = ColorStateList.valueOf(
                        getColorForAttr(androidx.appcompat.R.attr.colorError)
                    )
                }
                infoPrimary.setText(R.string.stats_report_status_partial_failure)
                if (report.errorMessage != null) {
                    infoSecondary.text = report.errorMessage
                }
            }
        }

        root.setOnClickListener { executionState.performClick() }
        executionState.setOnClickListener { item.onReportAction() }
    }

    data class Item(
        val report: Report,
        val tick: Any,
        val onReportAction: () -> Unit,
    ) : ReportsAdapter.Item {
        override val stableId: Long = report.reportId.hashCode().toLong()
    }
}