package com.campuseateries.service;

import com.campuseateries.dao.AnalyticsDao;
import com.campuseateries.model.AnalyticsRestaurantRow;
import com.campuseateries.model.MostActiveUserRow;
import com.campuseateries.model.TopMenuItemRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Admin-facing aggregator surfacing heavyweight SQL workloads through narrow Java contracts. */
public class AnalyticsDashboardService {

    private final AnalyticsDao dao = new AnalyticsDao();

    public List<TopMenuItemRow> topSelling(int topN, int trailingDaysWindow) throws Exception {
        return dao.fetchTopSellingMenuItems(topN, trailingDaysWindow);
    }

    public List<MostActiveUserRow> mostActiveUsers(int topN) throws Exception {
        return dao.fetchMostActiveUsers(topN);
    }

    public List<AnalyticsRestaurantRow> restaurantPerformanceRollup() throws Exception {
        return dao.restaurantPerformanceViaView();
    }

    public Map<String, Long> recentStatusCounts(int trailingHours) throws Exception {
        return dao.realTimeStatuses(trailingHours);
    }

    public BigDecimal dailyPaidTotals(LocalDate day) throws Exception {
        return dao.summarizeDailyPaidRevenue(day);
    }
}
