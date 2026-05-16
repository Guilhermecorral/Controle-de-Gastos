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
  const email = `qa.wishlist.${suffix}@test.com`;
  const password = "123456";

  const register = await callApi("POST", "/api/auth/register", {
    name: "QA Wishlist",
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

  const createTechList = await callApi("POST", "/api/wishlist/lists", {
    name: "Tecnologia",
    description: "Itens de tecnologia",
  }, token);

  const createGamesList = await callApi("POST", "/api/wishlist/lists", {
    name: "Jogos",
    description: "Lista de games",
  }, token);

  if (createTechList.status === 201 && createGamesList.status === 201) {
    add("Wishlist", "Create named lists", "PASS", "Listas Tecnologia e Jogos criadas");
  } else {
    add("Wishlist", "Create named lists", "FAIL", `${createTechList.status}/${createGamesList.status}`);
  }

  const allLists = await callApi("GET", "/api/wishlist/lists", undefined, token);

  if (Array.isArray(allLists.body) && allLists.body.length >= 3) {
    add("Wishlist", "List named lists", "PASS", `${allLists.body.length} listas encontradas incluindo a principal`);
  } else {
    add("Wishlist", "List named lists", "FAIL", JSON.stringify(allLists.body));
  }

  const notebook = await callApi("POST", "/api/wishlist", {
    description: "Notebook QA",
    originalPrice: 5000.0,
    discountPercent: 10.0,
    priority: "ALTO",
    category: "COMPRAS",
    notes: "Comprar em promocao",
    listId: createTechList.body.id,
  }, token);

  if (notebook.status === 201 && notebook.body?.finalPrice === 4500 && notebook.body?.listName === "Tecnologia") {
    add("Wishlist", "Create item in named list", "PASS", "Item criado na lista correta com desconto aplicado");
  } else {
    add("Wishlist", "Create item in named list", "FAIL", JSON.stringify(notebook.body));
  }

  const filteredTech = await callApi("GET", `/api/wishlist?listId=${createTechList.body.id}&status=PENDENTE&sortBy=PRIORIDADE`, undefined, token);

  if (Array.isArray(filteredTech.body) && filteredTech.body.length === 1 && filteredTech.body[0].description === "Notebook QA") {
    add("Wishlist", "Filter by list", "PASS", "Filtro por lista retornou o item esperado");
  } else {
    add("Wishlist", "Filter by list", "FAIL", JSON.stringify(filteredTech.body));
  }

  const purchased = await callApi("POST", `/api/wishlist/${notebook.body.id}/purchase`, {
    purchaseDate: "2026-07-19",
    paymentMethod: "CARTAO_CREDITO_PARCELADO",
    installments: 3,
    firstInstallmentNextMonth: false,
  }, token);

  if (purchased.status === 200 && purchased.body?.status === "COMPRADO" && purchased.body?.archivedAfterPurchase === true) {
    add("Wishlist", "Purchase parcelled item", "PASS", "Compra parcelada marcou item como comprado e arquivado");
  } else {
    add("Wishlist", "Purchase parcelled item", "FAIL", JSON.stringify(purchased.body));
  }

  const historyAfterPurchase = await callApi("GET", `/api/wishlist/${notebook.body.id}/history`, undefined, token);

  if (Array.isArray(historyAfterPurchase.body) && historyAfterPurchase.body[0]?.actionType === "PURCHASED") {
    add("Wishlist", "History after purchase", "PASS", `${historyAfterPurchase.body.length} eventos registrados`);
  } else {
    add("Wishlist", "History after purchase", "FAIL", JSON.stringify(historyAfterPurchase.body));
  }

  const undone = await callApi("POST", `/api/wishlist/${notebook.body.id}/undo-purchase`, undefined, token);

  if (undone.status === 200 && undone.body?.status === "PENDENTE" && undone.body?.archivedAfterPurchase === false) {
    add("Wishlist", "Undo purchase", "PASS", "Desfazer compra restaurou o item para pendente");
  } else {
    add("Wishlist", "Undo purchase", "FAIL", JSON.stringify(undone.body));
  }

  const historyAfterUndo = await callApi("GET", `/api/wishlist/${notebook.body.id}/history`, undefined, token);

  if (Array.isArray(historyAfterUndo.body) && historyAfterUndo.body[0]?.actionType === "PURCHASE_UNDONE") {
    add("Wishlist", "History after undo", "PASS", "Historico registrou o desfazer da compra");
  } else {
    add("Wishlist", "History after undo", "FAIL", JSON.stringify(historyAfterUndo.body));
  }

  const consoleItem = await callApi("POST", "/api/wishlist", {
    description: "Console QA",
    originalPrice: 3000.0,
    discountPercent: 0,
    priority: "MEDIA",
    category: "LAZER",
    notes: "Comprar no fim do ano",
    listId: createGamesList.body.id,
  }, token);

  const deleteGamesList = await callApi("DELETE", `/api/wishlist/lists/${createGamesList.body.id}`, undefined, token);

  if (deleteGamesList.status === 204) {
    const allItems = await callApi("GET", "/api/wishlist", undefined, token);
    const movedConsole = Array.isArray(allItems.body)
      ? allItems.body.find((item) => item.description === "Console QA")
      : null;

    if (movedConsole?.listName === "Lista Principal") {
      add("Wishlist", "Delete list and move items", "PASS", "Item foi realocado para a lista principal");
    } else {
      add("Wishlist", "Delete list and move items", "FAIL", JSON.stringify(movedConsole));
    }
  } else {
    add("Wishlist", "Delete list and move items", "FAIL", deleteGamesList.text);
  }

  const summary = await callApi("GET", "/api/wishlist/summary", undefined, token);

  if (summary.status === 200 && summary.body?.quantidadeItensDesejados >= 2) {
    add("Wishlist", "Wishlist summary", "PASS", `Resumo retornou ${summary.body.quantidadeItensDesejados} itens desejados`);
  } else {
    add("Wishlist", "Wishlist summary", "FAIL", JSON.stringify(summary.body));
  }

  console.log(JSON.stringify({ email, createdConsoleId: consoleItem.body?.id, results }, null, 2));
}

main().catch((error) => {
  console.error(String(error));
  process.exit(1);
});
