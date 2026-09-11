package com.salarymanagement.acceptance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end full-stack acceptance test for the Salary Management System REST API.
 *
 * <p>Verifies the complete HR Manager lifecycle over real HTTP network sockets against an isolated
 * in-memory SQLite database without modifying real development data or invoking seeder tasks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FullSystemAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    @DisplayName("HR Manager End-to-End Acceptance Journey")
    void hrManagerEndToEndAcceptanceJourney() throws Exception {
        HttpClient unauthClient = HttpClient.newHttpClient();
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient authClient = HttpClient.newBuilder().cookieHandler(cookieManager).build();

        // -------------------------------------------------------------------------
        // SECTION 1: Unauthenticated Security Enforcements
        // -------------------------------------------------------------------------
        // Given an unauthenticated client attempting to access protected dashboard
        HttpResponse<String> unauthDashboardResp = get(unauthClient, "/api/dashboard");

        // Then access is rejected with HTTP 401 Unauthorized
        assertEquals(401, unauthDashboardResp.statusCode(), "Unauthenticated dashboard query must return 401");

        // -------------------------------------------------------------------------
        // SECTION 2: HR Manager Authentication
        // -------------------------------------------------------------------------
        // When HR Manager submits valid credentials
        String loginPayload = """
                {
                    "username": "hr.manager",
                    "password": "changeme123!"
                }
                """;
        HttpResponse<String> loginResp = post(authClient, "/api/auth/login", loginPayload);

        // Then authentication succeeds and session cookie is stored
        assertEquals(200, loginResp.statusCode(), "Login must return 200 OK");
        assertFalse(cookieManager.getCookieStore().getCookies().isEmpty(), "HttpOnly auth cookie must be stored");

        // -------------------------------------------------------------------------
        // SECTION 3: Dashboard Insights & Aggregations
        // -------------------------------------------------------------------------
        // When HR Manager requests dashboard summary view
        HttpResponse<String> dashboardResp = get(authClient, "/api/dashboard");

        // Then overall metrics and multi-currency statistics are returned
        assertEquals(200, dashboardResp.statusCode(), "Dashboard query must return 200 OK");
        JsonNode dashboardJson = parseJson(dashboardResp);
        assertTrue(dashboardJson.has("totalEmployees"), "Dashboard must contain totalEmployees");
        assertTrue(dashboardJson.has("salaryMetricsByCurrency"), "Dashboard must contain salaryMetricsByCurrency");
        assertTrue(dashboardJson.has("employeesByCountry"), "Dashboard must contain employeesByCountry");

        // -------------------------------------------------------------------------
        // SECTION 4: Employee Registration & Search
        // -------------------------------------------------------------------------
        // Given a new employee registration request
        String createEmployeePayload = """
                {
                    "employeeNumber": "EMP-ACC-001",
                    "firstName": "Alex",
                    "lastName": "Rivera",
                    "email": "alex.rivera@company.com",
                    "country": "United States",
                    "department": "Engineering"
                }
                """;

        // When HR Manager registers the employee
        HttpResponse<String> createEmpResp = post(authClient, "/api/employees", createEmployeePayload);
        assertEquals(201, createEmpResp.statusCode(), "Employee creation must return 201 Created");
        JsonNode newEmployeeJson = parseJson(createEmpResp);
        long employeeId = newEmployeeJson.get("id").asLong();
        assertEquals("EMP-ACC-001", newEmployeeJson.get("employeeNumber").asText());

        // Then searching for the employee returns their record in paginated results
        HttpResponse<String> searchResp = get(authClient, "/api/employees?search=EMP-ACC-001&page=0&size=10");
        assertEquals(200, searchResp.statusCode(), "Employee search must return 200 OK");
        JsonNode searchJson = parseJson(searchResp);
        boolean foundCreatedEmployee = false;
        for (JsonNode item : searchJson.get("content")) {
            if ("EMP-ACC-001".equals(item.get("employeeNumber").asText())) {
                foundCreatedEmployee = true;
                break;
            }
        }
        assertTrue(foundCreatedEmployee, "Search results must contain employee with employeeNumber 'EMP-ACC-001'");

        // And employee details can be looked up individually
        HttpResponse<String> detailResp = get(authClient, "/api/employees/" + employeeId);
        assertEquals(200, detailResp.statusCode(), "Employee detail lookup must return 200 OK");
        JsonNode detailJson = parseJson(detailResp);
        assertEquals("Alex", detailJson.get("firstName").asText());
        assertEquals("Rivera", detailJson.get("lastName").asText());

        // -------------------------------------------------------------------------
        // SECTION 5: Salary Management & History Preservation
        // -------------------------------------------------------------------------
        // Given initial empty salary history
        HttpResponse<String> initHistoryResp = get(authClient, "/api/employees/" + employeeId + "/salary/history");
        assertEquals(200, initHistoryResp.statusCode());
        assertEquals(0, parseJson(initHistoryResp).size(), "Initial salary history should be empty");

        // When HR Manager records an initial salary (75,000 USD effective 2024-01-01)
        String salary1Payload = """
                {
                    "amount": 75000.00,
                    "currency": "USD",
                    "effectiveFrom": "2024-01-01"
                }
                """;
        HttpResponse<String> addSalaryResp1 = post(authClient, "/api/employees/" + employeeId + "/salary", salary1Payload);
        assertEquals(201, addSalaryResp1.statusCode(), "First salary creation must return 201 Created");

        // And HR Manager records a salary increase (92,000 USD effective 2025-01-01)
        String salary2Payload = """
                {
                    "amount": 92000.00,
                    "currency": "USD",
                    "effectiveFrom": "2025-01-01"
                }
                """;
        HttpResponse<String> addSalaryResp2 = post(authClient, "/api/employees/" + employeeId + "/salary", salary2Payload);
        assertEquals(201, addSalaryResp2.statusCode(), "Second salary creation must return 201 Created");

        // Then current active salary evaluates to the latest effective record (92,000 USD)
        HttpResponse<String> currentSalaryResp = get(authClient, "/api/employees/" + employeeId + "/salary");
        assertEquals(200, currentSalaryResp.statusCode(), "Current salary query must return 200 OK");
        JsonNode currentSalaryJson = parseJson(currentSalaryResp);
        assertEquals(92000.00, currentSalaryJson.get("amount").asDouble(), "Current salary must be latest effective date (92,000.00)");
        assertEquals("USD", currentSalaryJson.get("currency").asText());

        // And complete salary history is preserved in descending order without overwriting
        HttpResponse<String> fullHistoryResp = get(authClient, "/api/employees/" + employeeId + "/salary/history");
        assertEquals(200, fullHistoryResp.statusCode(), "Salary history query must return 200 OK");
        JsonNode fullHistoryJson = parseJson(fullHistoryResp);
        assertEquals(2, fullHistoryJson.size(), "Salary history must preserve both records");
        assertEquals(92000.00, fullHistoryJson.get(0).get("amount").asDouble(), "First history entry should be latest effective salary");
        assertEquals(75000.00, fullHistoryJson.get(1).get("amount").asDouble(), "Second history entry should be previous salary record");
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(HttpClient client, String path, String jsonBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode parseJson(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }
}
