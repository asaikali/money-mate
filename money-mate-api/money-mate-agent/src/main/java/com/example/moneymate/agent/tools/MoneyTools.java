package com.example.moneymate.agent.tools;

import org.springframework.ai.mcp.spec.McpSchema.McpTool;
import org.springframework.ai.mcp.spec.McpSchema.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class MoneyTools {

    @McpTool(
        name = "money-talk",
        description = "Conversational interface for discussing your finances, balances, transactions, and spending habits"
    )
    public String moneyTalk(
        @McpToolParam(
            description = "Your question or message about your finances",
            required = true
        ) String message
    ) {
        // Phase 1: Hardcoded response
        return "Your current balance is $1,234.56. You spent $45.67 on coffee this month.";
    }
}
