package com.viettel.sumo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    /**
     * Serves the traffic control dashboard
     * @return the view name (traffic-dashboard.html in templates folder)
     */
    @GetMapping("/dashboard/traffic-control")
    public String trafficControlDashboard() {
        return "traffic-dashboard";
    }
}
