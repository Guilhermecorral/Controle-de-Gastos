const baseUrl = "http://localhost:8080";

const results = [];

function add(area, test, status, details) {
  results.push({ area, test, status, details });
}

async function callApi(method, path, body, token) {
  const headers = { Accept: "application/json" };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(baseUrl + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  let parsed = null;

  if (text) {
    try {
      parsed = JSON.parse(text);
    } catch {
      parsed = text;
    }
  }

  return {
    status: response.status,
    body: parsed,
    text,
  };
}

async function main() {
  const suffix = Math.floor(Math.random() * 90000) + 10000;
  const email = `qa.finance.${suffix}@test.com`;
  const password = "123456";

  const register = await callApi("POST", "/api/auth/register", {
    name: "QA Finance",
    email,
    password,
  });

  if (register.status !== 201 || !register.body?.accessToken) {
    throw new Error(`Register failed: ${register.status} ${register.text}`);
  }

  add("Auth", "Register user", "PASS", email);

  const login = await callApi("POST", "/api/auth/login", {
    email,
    password,
  });

  if (login.status !== 200 || !login.body?.accessToken) {
    throw new Error(`Login failed: ${login.status} ${login.text}`);
  }

  add("Auth", "Login user", "PASS", "Token recebido");

  const token = login.body.accessToken;

  const seed = [
    { type: "RECEITA", description: "Salario Jan 2025", category: "OUTROS", amount: 2000.0, paymentMethod: "PIX", transactionDate: "2025-01-07" },
    { type: "DESPESA", description: "Conta Jan 2025", category: "COMPRAS", amount: 1000.0, paymentMethod: "DINHEIRO", transactionDate: "2025-01-20" },
    { type: "RECEITA", description: "Salario Abr 2025", category: "OUTROS", amount: 3800.0, paymentMethod: "PIX", transactionDate: "2025-04-05" },
    { type: "DESPESA", description: "Aluguel Abr 2025", category: "MORADIA", amount: 1600.0, paymentMethod: "PIX", transactionDate: "2025-04-10" },
    { type: "RECEITA", description: "Salario Mar 2026", category: "OUTROS", amount: 3000.0, paymentMethod: "PIX", transactionDate: "2026-03-05" },
    { type: "DESPESA", description: "Contas Mar 2026", category: "MORADIA", amount: 2200.0, paymentMethod: "PIX", transactionDate: "2026-03-10" },
    { type: "RECEITA", description: "Salario Abr 2026", category: "OUTROS", amount: 4000.0, paymentMethod: "PIX", transactionDate: "2026-04-05" },
    { type: "DESPESA", description: "Aluguel Abr 2026", category: "MORADIA", amount: 1200.0, paymentMethod: "PIX", transactionDate: "2026-04-10" },
    { type: "DESPESA", description: "Mercado Abr 2026", category: "ALIMENTACAO", amount: 300.0, paymentMethod: "CARTAO_DEBITO", transactionDate: "2026-04-12" },
    { type: "RECEITA", description: "Salario Mai 2026", category: "OUTROS", amount: 4500.0, paymentMethod: "PIX", transactionDate: "2026-05-05" },
    { type: "DESPESA", description: "Uber Mai 2026", category: "TRANSPORTE", amount: 150.0, paymentMethod: "PIX", transactionDate: "2026-05-11" },
  ];

  const created = [];

  for (const transaction of seed) {
    const response = await callApi("POST", "/api/transactions", transaction, token);

    if (response.status !== 201) {
      throw new Error(`Create transaction failed: ${response.status} ${response.text}`);
    }

    created.push(response.body);
  }

  add("Transactions", "Create seed transactions", "PASS", `${created.length} criadas`);

  const all = await callApi("GET", "/api/transactions", undefined, token);

  if (Array.isArray(all.body) && all.body.length >= 11) {
    add("Transactions", "List all transactions", "PASS", `${all.body.length} listadas`);
  } else {
    add("Transactions", "List all transactions", "FAIL", JSON.stringify(all.body));
  }

  const expenses = await callApi("GET", "/api/transactions?type=DESPESA", undefined, token);

  if (Array.isArray(expenses.body) && expenses.body.every((item) => item.type === "DESPESA")) {
    add("Transactions", "Filter by type", "PASS", `${expenses.body.length} despesas coerentes`);
  } else {
    add("Transactions", "Filter by type", "FAIL", JSON.stringify(expenses.body));
  }

  const market = all.body.find((item) => item.description === "Mercado Abr 2026");
  const updatedMarket = await callApi(
    "PUT",
    `/api/transactions/${market.id}`,
    {
      type: "DESPESA",
      description: "Mercado Abr 2026 Atualizado",
      category: "ALIMENTACAO",
      amount: 350.0,
      paymentMethod: "CARTAO_DEBITO",
      transactionDate: "2026-04-12",
    },
    token
  );

  if (updatedMarket.body?.amount === 350 && updatedMarket.body?.description === "Mercado Abr 2026 Atualizado") {
    add("Transactions", "Update transaction", "PASS", "Valor atualizado para 350");
  } else {
    add("Transactions", "Update transaction", "FAIL", JSON.stringify(updatedMarket.body));
  }

  const uber = all.body.find((item) => item.description === "Uber Mai 2026");
  const deleteResponse = await callApi("DELETE", `/api/transactions/${uber.id}`, undefined, token);

  if (deleteResponse.status === 204) {
    add("Transactions", "Delete transaction", "PASS", "Retornou 204");
  } else {
    add("Transactions", "Delete transaction", "FAIL", deleteResponse.text);
  }

  const dashboardApril = await callApi("GET", "/api/dashboard?year=2026&month=4", undefined, token);

  if (
    dashboardApril.body?.receitasMesAtual === 4000 &&
    dashboardApril.body?.despesasMesAtual === 1550 &&
    dashboardApril.body?.resultadoMesAtual === 2450
  ) {
    add("Dashboard", "April 2026 reference", "PASS", "Totais esperados");
  } else {
    add("Dashboard", "April 2026 reference", "FAIL", JSON.stringify(dashboardApril.body));
  }

  const dashboardMay = await callApi("GET", "/api/dashboard?year=2026&month=5", undefined, token);

  if (
    dashboardMay.body?.receitasMesAtual === 4500 &&
    dashboardMay.body?.despesasMesAtual === 0 &&
    dashboardMay.body?.resultadoMesAtual === 4500
  ) {
    add("Dashboard", "May 2026 reference", "PASS", "Despesa deletada nao contaminou maio");
  } else {
    add("Dashboard", "May 2026 reference", "FAIL", JSON.stringify(dashboardMay.body));
  }

  const analysis = await callApi("GET", "/api/monthly-analysis?year=2026&month=4", undefined, token);

  if (
    analysis.body?.totalReceitas === 4000 &&
    analysis.body?.totalDespesas === 1550 &&
    analysis.body?.saldo === 2450 &&
    analysis.body?.comparativoMesAnterior?.saldo === 800 &&
    analysis.body?.comparativoMesmoMesAnoAnterior?.saldo === 2200
  ) {
    add("MonthlyAnalysis", "April 2026 analysis", "PASS", "Comparacoes mensal e anual coerentes");
  } else {
    add("MonthlyAnalysis", "April 2026 analysis", "FAIL", JSON.stringify(analysis.body));
  }

  console.log(JSON.stringify({ email, results }, null, 2));
}

main().catch((error) => {
  console.error(String(error));
  process.exit(1);
});
