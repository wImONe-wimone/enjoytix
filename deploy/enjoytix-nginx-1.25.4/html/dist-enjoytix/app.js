(function () {
  const SUCCESS_CODE = "0";
  const STORAGE_TOKEN = "enjoytix.token";
  const STORAGE_USER = "enjoytix.user";
  const STORAGE_CONTEXT = "enjoytix.checkoutContext";
  const USER_ID_HEADER = "X-User-Id";
  const AUTHORIZATION_PREFIX = "Bearer ";
  const MAX_QUANTITY = 6;
  const API_BASE = normalizeBaseUrl(window.ENJOYTIX_API_BASE || "");

  const app = document.querySelector("#app");
  const authRoot = document.querySelector("#auth-root");
  const dialogRoot = document.querySelector("#dialog-root");
  const toastRoot = document.querySelector("#toast-root");

  const state = {
    token: "",
    user: null,
    catalogQuery: {},
    currentDetail: null,
    detailSelection: {},
    checkoutPay: {},
    busy: false
  };

  localStorage.removeItem(STORAGE_TOKEN);
  localStorage.removeItem(STORAGE_USER);

  window.addEventListener("hashchange", route);
  document.addEventListener("click", handleGlobalClick);

  renderAuth();
  route();
  if (state.token) {
    refreshUser();
  }

  function handleGlobalClick(event) {
    const action = event.target.closest("[data-action]");
    if (!action) {
      return;
    }
    const name = action.dataset.action;
    if (name === "open-login") {
      openAuthDialog("login");
    }
    if (name === "open-register") {
      openAuthDialog("register");
    }
    if (name === "close-dialog") {
      closeDialog();
    }
    if (name === "logout") {
      logout();
    }
  }

  function route() {
    const { path, query } = parseHash();
    updateNav(path);
    app.focus({ preventScroll: true });

    if (path === "/" || path === "") {
      renderCatalog(query);
      return;
    }
    if (path.startsWith("/performance/")) {
      renderPerformanceDetail(path.split("/")[2], query);
      return;
    }
    if (path.startsWith("/checkout/")) {
      renderCheckout(path.split("/")[2]);
      return;
    }
    if (path.startsWith("/tickets/")) {
      renderTickets(path.split("/")[2]);
      return;
    }
    if (path === "/orders") {
      renderOrders();
      return;
    }
    if (path === "/account" || path.startsWith("/account/")) {
      renderAccount(path.split("/")[2] || query.tab || "profile");
      return;
    }
    renderNotFound();
  }

  async function renderCatalog(query) {
    state.catalogQuery = {
      keyword: query.keyword || "",
      performanceType: query.performanceType || "",
      showDate: query.showDate || "",
      current: Number(query.current || 1),
      size: 12
    };
    renderLoading("正在加载文娱票务项目");
    try {
      const params = new URLSearchParams();
      params.set("current", state.catalogQuery.current);
      params.set("size", state.catalogQuery.size);
      if (state.catalogQuery.performanceType) params.set("performanceType", state.catalogQuery.performanceType);
      if (state.catalogQuery.keyword) params.set("artistName", state.catalogQuery.keyword);
      if (state.catalogQuery.showDate) params.set("showDate", state.catalogQuery.showDate);
      const page = await apiGet(`/api/performance/page?${params.toString()}`);
      app.innerHTML = catalogTemplate(page, state.catalogQuery);
      bindCatalog(page);
    } catch (error) {
      renderError("项目列表加载失败", error, () => renderCatalog(query));
    }
  }

  function catalogTemplate(page, query) {
    const records = Array.isArray(page.records) ? page.records : [];
    const total = Number(page.total || 0);
    const maxPage = Math.max(1, Math.ceil(total / query.size));
    return `
      <section class="band">
        <div class="section-title">
          <div>
            <h1>文娱票务项目</h1>
            <p>共 ${total} 个可购项目</p>
          </div>
          <div class="button-row">
            <a class="btn" href="#/orders">查看订单</a>
          </div>
        </div>
        <form id="catalog-form" class="toolbar">
          <div class="field">
            <label for="keyword">艺人/团队</label>
            <input id="keyword" name="keyword" value="${escapeAttr(query.keyword)}" placeholder="Aurora / Theatre">
          </div>
          <div class="field">
            <label for="performanceType">类型</label>
            <select id="performanceType" name="performanceType">
              <option value="">全部</option>
              ${option("CONCERT", "演唱会", query.performanceType)}
              ${option("DRAMA", "戏剧", query.performanceType)}
              ${option("MOVIE", "电影", query.performanceType)}
            </select>
          </div>
          <div class="field">
            <label for="showDate">日期</label>
            <input id="showDate" name="showDate" type="date" value="${escapeAttr(query.showDate)}">
          </div>
          <div class="field">
            <label>&nbsp;</label>
            <button class="btn primary" type="submit">筛选</button>
          </div>
        </form>
        ${records.length ? `
          <div class="performance-grid">
            ${records.map(performanceCard).join("")}
          </div>
          <div class="button-row">
            <button class="btn" data-page="${query.current - 1}" ${query.current <= 1 ? "disabled" : ""}>上一页</button>
            <span class="muted">第 ${query.current} / ${maxPage} 页</span>
            <button class="btn" data-page="${query.current + 1}" ${query.current >= maxPage ? "disabled" : ""}>下一页</button>
          </div>
        ` : emptyState("没有匹配项目", "换一个日期、类型或艺人/团队再试。")}
      </section>
    `;
  }

  function performanceCard(item) {
    const venue = item.venue || {};
    const venueAddress = formatVenueAddress(venue) || "-";
    return `
      <article class="performance-card">
        <a class="poster" href="#/performance/${item.performanceId}" aria-label="${escapeAttr(item.title)}">
          <img src="${posterFor(item)}" alt="${escapeAttr(item.title)}海报" loading="lazy" onerror="this.onerror=null;this.src='/assets/posters/generic.png';">
          <span class="poster-badge pill ok">${typeText(item.performanceType)}</span>
        </a>
        <div class="performance-body">
          <h3><a href="#/performance/${item.performanceId}">${escapeHtml(item.title)}</a></h3>
          <ul class="meta-list">
            <li><strong>${escapeHtml(venue.name || "-")}</strong></li>
            <li>${escapeHtml(venueAddress)}</li>
            <li>${escapeHtml(item.artist?.name || "-")}</li>
            <li>${formatDateTime(item.earliestShowTime)}</li>
          </ul>
          <a class="btn primary" href="#/performance/${item.performanceId}">选购门票</a>
        </div>
      </article>
    `;
  }

  function bindCatalog(page) {
    const form = document.querySelector("#catalog-form");
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      const data = Object.fromEntries(new FormData(form).entries());
      const params = new URLSearchParams();
      for (const key of ["keyword", "performanceType", "showDate"]) {
        if (data[key]) params.set(key, data[key]);
      }
      window.location.hash = `#/${params.toString() ? `?${params.toString()}` : ""}`;
    });
    document.querySelectorAll("[data-page]").forEach((button) => {
      button.addEventListener("click", () => {
        const targetPage = Number(button.dataset.page);
        if (!targetPage || targetPage < 1) return;
        const params = new URLSearchParams();
        for (const key of ["keyword", "performanceType", "showDate"]) {
          if (state.catalogQuery[key]) params.set(key, state.catalogQuery[key]);
        }
        params.set("current", targetPage);
        window.location.hash = `#/?${params.toString()}`;
      });
    });
  }

  async function renderPerformanceDetail(performanceId, query) {
    renderLoading("正在加载项目详情");
    try {
      const detail = await apiGet(`/api/performance/${encodeURIComponent(performanceId)}`);
      const showId = Number(query.showId || detail.sessions?.[0]?.showId || 0);
      if (!showId) {
        app.innerHTML = emptyState("暂无可售时间", "该项目还没有开放售票时间。");
        return;
      }
      const [availability, seatMap, seats] = await Promise.all([
        apiGet(`/api/ticket/availability?showId=${encodeURIComponent(showId)}`),
        apiGet(`/api/show/${encodeURIComponent(showId)}/seat-map`),
        apiGet(`/api/ticket/seats?showId=${encodeURIComponent(showId)}`)
      ]);
      state.currentDetail = { detail, showId, availability, seatMap, seats };
      ensureSelection(showId, availability);
      renderDetailFromState();
    } catch (error) {
      renderError("项目详情加载失败", error, () => renderPerformanceDetail(performanceId, query));
    }
  }

  function renderDetailFromState() {
    const data = state.currentDetail;
    if (!data) return;
    const { detail, showId, availability, seatMap, seats } = data;
    const selection = ensureSelection(showId, availability);
    const show = detail.sessions.find((item) => Number(item.showId) === Number(showId)) || detail.sessions[0];
    const category = availability.find((item) => Number(item.categoryId) === Number(selection.categoryId));
    const selectedSeats = selectedSeatDetails(seats, selection.selectedSeats);
    const venueAddress = formatVenueAddress(detail.venue) || "-";
    app.innerHTML = `
      <section class="detail-layout">
        <div class="detail-main">
          <div class="detail-intro">
            <div class="poster">
              <img src="${posterFor(detail)}" alt="${escapeAttr(detail.title)}海报" onerror="this.onerror=null;this.src='/assets/posters/generic.png';">
            </div>
            <div class="detail-copy">
              <div class="button-row">
                <a class="btn ghost compact" href="#/">返回项目</a>
                <span class="pill ok">${typeText(detail.performanceType)}</span>
              </div>
              <h1>${escapeHtml(detail.title)}</h1>
              <p>${escapeHtml(detail.description || "")}</p>
              <ul class="meta-list">
                <li><strong>${escapeHtml(detail.venue?.name || "-")}</strong> · ${escapeHtml(venueAddress)}</li>
                <li>${escapeHtml(detail.artist?.name || "-")}</li>
              </ul>
            </div>
          </div>
          <section class="panel">
            <h2>选择演出时间</h2>
            <div class="segmented">
              ${(detail.sessions || []).map((item) => `
                <button type="button" data-show-id="${item.showId}" class="${Number(item.showId) === Number(showId) ? "active" : ""}">
                  <strong>${formatDateTime(item.showTime)}</strong><br>
                  <span class="muted">停售 ${formatDateTime(item.saleEndTime)}</span>
                </button>
              `).join("")}
            </div>
          </section>
          <section id="category-panel" class="panel">
            ${categoryPanelTemplate(availability, selection.categoryId)}
          </section>
          <section id="seat-map-panel" class="panel">
            ${seatMapTemplate(seatMap, seats, category, selection)}
          </section>
        </div>
        <aside id="checkout-panel" class="panel summary-panel">
          ${selectionSummaryTemplate(detail, show, category, selectedSeats, selection)}
        </aside>
      </section>
    `;
    bindDetailEvents();
  }

  function categoryPanelTemplate(availability, selectedCategoryId) {
    if (!availability.length) {
      return emptyState("暂无票档", "该演出时间当前没有可售票档。");
    }
    return `
      <h2>选择票档</h2>
      <div class="category-list">
        ${availability.map((item) => `
          <button type="button" class="category-button ${Number(item.categoryId) === Number(selectedCategoryId) ? "active" : ""}" data-category-id="${item.categoryId}" ${item.availableStock <= 0 ? "disabled" : ""}>
            <strong>${escapeHtml(item.categoryName)}</strong>
            <span>${formatMoney(item.price)} · 可售 ${item.availableStock}</span>
            <span>${item.seatSelectable === 1 ? "可选座" : "不选座"}</span>
          </button>
        `).join("")}
      </div>
    `;
  }

  function seatMapTemplate(seatMap, seats, category, selection) {
    if (!category) {
      return emptyState("请选择票档", "选择票档后可继续选座或填写数量。");
    }
    if (category.seatSelectable !== 1) {
      return `
        <h2>填写数量</h2>
        <div class="button-row">
          <div class="quantity" aria-label="购票数量">
            <button type="button" data-quantity="-1">-</button>
            <input id="quantity" value="${selection.quantity}" inputmode="numeric" aria-label="数量">
            <button type="button" data-quantity="1">+</button>
          </div>
          <span class="muted">单笔最多 ${MAX_QUANTITY} 张</span>
        </div>
      `;
    }
    if (!seatMap || !Array.isArray(seatMap.seats) || !seatMap.seats.length) {
      return emptyState("暂无座位图", "该演出时间还没有座位图数据。");
    }
    const seatState = new Map(seats.map((seat) => [String(seat.seatId), seat]));
    const rows = [];
    for (let row = 1; row <= Number(seatMap.rowCount || 0); row += 1) {
      const cells = [];
      for (let column = 1; column <= Number(seatMap.columnCount || 0); column += 1) {
        const seat = seatMap.seats.find((item) => Number(item.rowNo) === row && Number(item.columnNo) === column);
        cells.push(seatButton(seat, seatState, category, selection));
      }
      rows.push(`<div class="seat-row" style="grid-template-columns: repeat(${Number(seatMap.columnCount || 1)}, minmax(32px, 1fr));">${cells.join("")}</div>`);
    }
    return `
      <div class="seat-wrap">
        <div class="section-title">
          <div>
            <h2>${escapeHtml(seatMap.name || "座位图")}</h2>
            <p>已选 ${selection.selectedSeats.length} / ${MAX_QUANTITY} 个座位</p>
          </div>
          <button class="btn compact" type="button" data-refresh-seats>刷新座位</button>
        </div>
        <div class="stage">舞台 / 银幕</div>
        <div class="seat-map">${rows.join("")}</div>
        <div class="legend">
          <span><i class="swatch"></i>可选</span>
          <span><i class="swatch selected"></i>已选</span>
          <span><i class="swatch locked"></i>已锁定</span>
          <span><i class="swatch sold"></i>已售</span>
        </div>
      </div>
    `;
  }

  function seatButton(seat, seatState, category, selection) {
    if (!seat) {
      return `<button type="button" class="seat missing" disabled aria-label="空位"></button>`;
    }
    const realtime = seatState.get(String(seat.seatId));
    const status = realtime?.status || "UNAVAILABLE";
    const categoryMatches = realtime && Number(realtime.categoryId) === Number(category.categoryId);
    const selected = selection.selectedSeats.includes(Number(seat.seatId));
    const available = status === "AVAILABLE" && categoryMatches;
    const classNames = ["seat"];
    if (selected) classNames.push("selected");
    if (status === "LOCKED") classNames.push("locked");
    if (status === "SOLD") classNames.push("sold");
    if (!categoryMatches && status === "AVAILABLE") classNames.push("other");
    const disabled = selected ? "" : (available ? "" : "disabled");
    const label = `${seat.seatNo} ${statusText(status)}`;
    return `
      <button type="button" class="${classNames.join(" ")}" data-seat-id="${seat.seatId}" ${disabled} title="${escapeAttr(label)}" aria-label="${escapeAttr(label)}" aria-pressed="${selected ? "true" : "false"}">
        ${escapeHtml(shortSeatNo(seat.seatNo))}
      </button>
    `;
  }

  function selectionSummaryTemplate(detail, show, category, selectedSeats, selection) {
    const quantity = category?.seatSelectable === 1 ? selection.selectedSeats.length : selection.quantity;
    const amount = Number(category?.price || 0) * quantity;
    const canSubmit = category && quantity > 0 && Number(category.availableStock || 0) >= quantity;
    return `
      <h2>订单确认</h2>
      <div class="summary-line"><span>项目</span><strong>${escapeHtml(detail.title)}</strong></div>
      <div class="summary-line"><span>演出时间</span><strong>${formatDateTime(show?.showTime)}</strong></div>
      <div class="summary-line"><span>票档</span><strong>${escapeHtml(category?.categoryName || "-")}</strong></div>
      <div class="summary-line"><span>座位</span><strong>${selectedSeats.length ? selectedSeats.map((seat) => escapeHtml(seat.seatNo)).join("、") : "未选择"}</strong></div>
      <div class="summary-line"><span>数量</span><strong>${quantity}</strong></div>
      <div class="summary-line"><span>合计</span><strong class="price">${formatMoney(amount)}</strong></div>
      <button class="btn primary" type="button" data-create-order ${canSubmit && !state.busy ? "" : "disabled"}>
        ${state.busy ? "提交中" : "确认锁座并下单"}
      </button>
      <p class="muted">待支付订单会显示锁定到期时间，超时后座位自动释放。</p>
    `;
  }

  function bindDetailEvents() {
    document.querySelectorAll("[data-show-id]").forEach((button) => {
      button.addEventListener("click", () => {
        const performanceId = state.currentDetail.detail.performanceId;
        window.location.hash = `#/performance/${performanceId}?showId=${button.dataset.showId}`;
      });
    });
    document.querySelectorAll("[data-category-id]").forEach((button) => {
      button.addEventListener("click", () => {
        const showId = state.currentDetail.showId;
        const selection = ensureSelection(showId, state.currentDetail.availability);
        selection.categoryId = Number(button.dataset.categoryId);
        selection.selectedSeats = [];
        selection.quantity = 1;
        renderDetailFromState();
      });
    });
    document.querySelectorAll("[data-seat-id]").forEach((button) => {
      button.addEventListener("click", () => {
        const showId = state.currentDetail.showId;
        const selection = ensureSelection(showId, state.currentDetail.availability);
        const seatId = Number(button.dataset.seatId);
        if (selection.selectedSeats.includes(seatId)) {
          selection.selectedSeats = selection.selectedSeats.filter((item) => item !== seatId);
        } else if (selection.selectedSeats.length < MAX_QUANTITY) {
          selection.selectedSeats.push(seatId);
        } else {
          toast(`单笔最多选择 ${MAX_QUANTITY} 个座位`, "error");
        }
        renderDetailFromState();
      });
    });
    document.querySelectorAll("[data-quantity]").forEach((button) => {
      button.addEventListener("click", () => {
        const showId = state.currentDetail.showId;
        const selection = ensureSelection(showId, state.currentDetail.availability);
        selection.quantity = clamp(Number(selection.quantity || 1) + Number(button.dataset.quantity), 1, MAX_QUANTITY);
        renderDetailFromState();
      });
    });
    const quantity = document.querySelector("#quantity");
    if (quantity) {
      quantity.addEventListener("change", () => {
        const showId = state.currentDetail.showId;
        const selection = ensureSelection(showId, state.currentDetail.availability);
        selection.quantity = clamp(Number(quantity.value || 1), 1, MAX_QUANTITY);
        renderDetailFromState();
      });
    }
    const refresh = document.querySelector("[data-refresh-seats]");
    if (refresh) {
      refresh.addEventListener("click", refreshCurrentSeats);
    }
    const create = document.querySelector("[data-create-order]");
    if (create) {
      create.addEventListener("click", createOrderFromSelection);
    }
  }

  async function refreshCurrentSeats() {
    if (!state.currentDetail) return;
    try {
      const showId = state.currentDetail.showId;
      const [availability, seats] = await Promise.all([
        apiGet(`/api/ticket/availability?showId=${encodeURIComponent(showId)}`),
        apiGet(`/api/ticket/seats?showId=${encodeURIComponent(showId)}`)
      ]);
      state.currentDetail.availability = availability;
      state.currentDetail.seats = seats;
      pruneUnavailableSeats();
      renderDetailFromState();
      toast("座位状态已刷新", "success");
    } catch (error) {
      toast(apiMessage(error), "error");
    }
  }

  function pruneUnavailableSeats() {
    const data = state.currentDetail;
    const selection = ensureSelection(data.showId, data.availability);
    const categoryId = Number(selection.categoryId);
    const seatMap = new Map(data.seats.map((seat) => [Number(seat.seatId), seat]));
    selection.selectedSeats = selection.selectedSeats.filter((seatId) => {
      const seat = seatMap.get(Number(seatId));
      return seat && seat.status === "AVAILABLE" && Number(seat.categoryId) === categoryId;
    });
  }

  async function createOrderFromSelection() {
    if (!state.currentDetail || state.busy) return;
    if (!requireLogin()) return;
    const data = state.currentDetail;
    const selection = ensureSelection(data.showId, data.availability);
    const category = data.availability.find((item) => Number(item.categoryId) === Number(selection.categoryId));
    const quantity = category?.seatSelectable === 1 ? selection.selectedSeats.length : selection.quantity;
    if (!category || quantity < 1) {
      toast("请选择票档和座位", "error");
      return;
    }
    state.busy = true;
    renderDetailFromState();
    try {
      const payload = {
        showId: Number(data.showId),
        categoryId: Number(category.categoryId),
        quantity: Number(quantity),
        seatIds: category.seatSelectable === 1 ? selection.selectedSeats : []
      };
      const order = await apiPost("/api/order/create", payload);
      const orderId = resolveOrderId(order);
      if (!orderId) {
        throw new Error("订单创建结果缺少 order_id");
      }
      rememberContext(orderId, {
        performanceId: data.detail.performanceId,
        performanceTitle: data.detail.title,
        showId: data.showId,
        categoryId: category.categoryId,
        categoryName: category.categoryName,
        showTime: (data.detail.sessions.find((item) => Number(item.showId) === Number(data.showId)) || {}).showTime,
        seats: selectedSeatDetails(data.seats, selection.selectedSeats)
      });
      toast("座位已锁定，订单待支付", "success");
      window.location.hash = checkoutHash(orderId);
    } catch (error) {
      toast(lockErrorMessage(error), "error");
      await refreshCurrentSeats();
    } finally {
      state.busy = false;
      if (location.hash.startsWith("#/performance/")) {
        renderDetailFromState();
      }
    }
  }

  async function renderCheckout(orderId) {
    const routeOrderId = normalizeId(orderId);
    if (!requireLogin()) return;
    renderLoading("正在加载订单");
    try {
      const order = await apiGet(`/api/user/orders/${encodeURIComponent(routeOrderId)}`);
      const resolvedOrderId = resolveOrderId(order, routeOrderId);
      const context = readContext(resolvedOrderId);
      app.innerHTML = checkoutTemplate(order, context, state.checkoutPay[resolvedOrderId], resolvedOrderId);
      bindCheckout(order, resolvedOrderId);
    } catch (error) {
      renderError("订单加载失败", error, () => renderCheckout(routeOrderId));
    }
  }

  function checkoutTemplate(order, context, pay, orderId) {
    const resolvedOrderId = resolveOrderId(order, orderId);
    const payId = resolvePayId(pay);
    const paid = order.status === "PAID";
    const pending = order.status === "PENDING_PAYMENT";
    return `
      <section class="checkout-layout">
        <div class="detail-main">
          <div class="section-title">
            <div>
              <h1>订单 ${escapeHtml(order.orderSn || order.orderId)}</h1>
              <p>${context?.performanceTitle ? escapeHtml(context.performanceTitle) : `演出时间 #${escapeHtml(order.showId)}`}</p>
            </div>
            ${statusPill(order.status)}
          </div>
          <section class="panel">
            <h2>明细</h2>
            <ul class="meta-list">
              <li><strong>订单号</strong> ${escapeHtml(order.orderSn || "-")}</li>
              <li><strong>锁单号</strong> ${escapeHtml(order.lockId || "-")}</li>
              <li><strong>支付截止</strong> ${formatDateTime(order.payExpireTime)}</li>
              <li><strong>应付金额</strong> ${formatMoney(order.totalAmount)}</li>
            </ul>
          </section>
          <section class="panel">
            <h2>订单项目</h2>
            <div class="order-list">
              ${(order.items || []).map((item) => `
                <article class="state-box">
                  <h3>${escapeHtml(context?.categoryName || `票档 ${item.categoryId}`)}</h3>
                  <p>${item.quantity} 张 · 单价 ${formatMoney(item.unitPrice)} · 小计 ${formatMoney(item.amount)}</p>
                  <p>座位：${item.seatIds?.length ? item.seatIds.map((id) => escapeHtml(seatLabelFromContext(context, id))).join("、") : "系统分配"}</p>
                  ${item.ticketCodes?.length ? `<p>票码：${item.ticketCodes.map(escapeHtml).join("、")}</p>` : ""}
                </article>
              `).join("")}
            </div>
          </section>
        </div>
        <aside class="panel summary-panel">
          <h2>支付状态</h2>
          <ol class="timeline">
            <li class="done"><span class="dot">1</span><div><strong>锁座下单</strong><br><span class="muted">${formatDateTime(order.payExpireTime)} 前有效</span></div></li>
            <li class="${pay || paid ? "done" : ""}"><span class="dot">2</span><div><strong>创建支付单</strong><br><span class="muted">${pay ? escapeHtml(pay.paySn) : "等待创建"}</span></div></li>
            <li class="${paid ? "done" : ""}"><span class="dot">3</span><div><strong>出票完成</strong><br><span class="muted">${paid ? "可查看电子票" : "支付成功后出票"}</span></div></li>
          </ol>
          <div class="summary-line"><span>金额</span><strong class="price">${formatMoney(order.totalAmount)}</strong></div>
          ${pending ? `
            <button class="btn primary" type="button" data-pay-create="${escapeAttr(resolvedOrderId)}" ${state.busy ? "disabled" : ""}>${pay ? "刷新支付单" : "创建支付单"}</button>
            ${pay ? `<button class="btn primary" type="button" data-pay-success="${escapeAttr(payId)}" ${state.busy ? "disabled" : ""}>模拟支付成功</button>` : ""}
            <button class="btn danger" type="button" data-order-cancel="${escapeAttr(resolvedOrderId)}" ${state.busy ? "disabled" : ""}>取消订单</button>
          ` : ""}
          ${paid ? `<a class="btn primary" href="${ticketsHash(resolvedOrderId)}">查看电子票</a>` : ""}
          ${!pending && !paid ? `<p class="muted">当前状态不可继续支付。</p>` : ""}
        </aside>
      </section>
    `;
  }

  function bindCheckout(order, orderId) {
    const resolvedOrderId = resolveOrderId(order, orderId);
    const createPay = document.querySelector("[data-pay-create]");
    if (createPay) {
      createPay.addEventListener("click", () => createPayOrder(resolvedOrderId));
    }
    const mockPay = document.querySelector("[data-pay-success]");
    if (mockPay) {
      mockPay.addEventListener("click", () => mockPaySuccess(mockPay.dataset.paySuccess, resolvedOrderId));
    }
    const cancel = document.querySelector("[data-order-cancel]");
    if (cancel) {
      cancel.addEventListener("click", () => cancelOrder(resolvedOrderId));
    }
  }

  async function createPayOrder(orderId) {
    if (!requireLogin()) return;
    const resolvedOrderId = normalizeId(orderId);
    state.busy = true;
    setActionButtonsDisabled(true);
    try {
      const pay = await apiPost("/api/pay/create", { orderId: resolvedOrderId });
      state.checkoutPay[resolvedOrderId] = pay;
      toast("支付单已创建", "success");
    } catch (error) {
      toast(apiMessage(error), "error");
    } finally {
      state.busy = false;
      renderCheckout(resolvedOrderId);
    }
  }

  async function mockPaySuccess(payId, orderId) {
    if (!requireLogin()) return;
    const resolvedOrderId = normalizeId(orderId);
    const resolvedPayId = normalizeId(payId);
    state.busy = true;
    setActionButtonsDisabled(true);
    try {
      await apiPost("/api/pay/mock-success", { payId: resolvedPayId });
      toast("模拟支付成功，电子票已生成", "success");
      state.busy = false;
      window.location.hash = ticketsHash(resolvedOrderId);
    } catch (error) {
      state.busy = false;
      toast(apiMessage(error), "error");
      renderCheckout(resolvedOrderId);
    }
  }

  async function cancelOrder(orderId) {
    if (!requireLogin()) return;
    const resolvedOrderId = normalizeId(orderId);
    if (!window.confirm("确认取消该待支付订单？")) {
      return;
    }
    try {
      await apiPost("/api/user/orders/cancel", { orderId: resolvedOrderId });
      toast("订单已取消，座位已释放", "success");
      renderCheckout(resolvedOrderId);
    } catch (error) {
      toast(apiMessage(error), "error");
    }
  }

  async function renderTickets(orderId) {
    const routeOrderId = normalizeId(orderId);
    if (!requireLogin()) return;
    renderLoading("正在加载电子票");
    try {
      const order = await apiGet(`/api/user/orders/${encodeURIComponent(routeOrderId)}`);
      const resolvedOrderId = resolveOrderId(order, routeOrderId);
      const show = await apiGet(`/api/show/${encodeURIComponent(order.showId)}`).catch(() => null);
      const performance = show?.performanceId ? await apiGet(`/api/performance/${encodeURIComponent(show.performanceId)}`).catch(() => null) : null;
      app.innerHTML = ticketsTemplate(order, show, performance, readContext(resolvedOrderId), resolvedOrderId);
    } catch (error) {
      renderError("电子票加载失败", error, () => renderTickets(routeOrderId));
    }
  }

  function ticketsTemplate(order, show, performance, context, orderId) {
    const resolvedOrderId = resolveOrderId(order, orderId);
    const tickets = [];
    for (const item of order.items || []) {
      (item.ticketCodes || []).forEach((code, index) => {
        tickets.push({
          code,
          item,
          seatId: item.seatIds?.[index] || null
        });
      });
    }
    if (order.status !== "PAID") {
      return `
        <section class="band">
          <div class="section-title">
            <div>
              <h1>电子票</h1>
              <p>订单 ${escapeHtml(order.orderSn || order.orderId)}</p>
            </div>
            ${statusPill(order.status)}
          </div>
          ${emptyState("尚未出票", "订单支付成功后会生成电子票。")}
          <div class="button-row">
            <a class="btn primary" href="${checkoutHash(resolvedOrderId)}">返回订单</a>
            <a class="btn" href="#/orders">订单列表</a>
          </div>
        </section>
      `;
    }
    return `
      <section class="band">
        <div class="section-title">
          <div>
            <h1>电子票</h1>
            <p>${escapeHtml(performance?.title || context?.performanceTitle || `演出时间 #${order.showId}`)}</p>
          </div>
          ${statusPill(order.status)}
        </div>
        <div class="ticket-grid">
          ${tickets.map((ticket, index) => `
            <article class="ticket-card">
              <div class="ticket-top">
                <h3>${escapeHtml(performance?.title || context?.performanceTitle || "EnjoyTix")}</h3>
                <span class="pill ok">已出票</span>
              </div>
              <span class="ticket-code">${escapeHtml(ticket.code)}</span>
              <ul class="meta-list">
                <li><strong>订单</strong> ${escapeHtml(order.orderSn || order.orderId)}</li>
                <li><strong>时间</strong> ${formatDateTime(show?.showTime || context?.showTime)}</li>
                <li><strong>场馆</strong> ${escapeHtml(performance?.venue?.name || "-")}</li>
                <li><strong>票档</strong> ${escapeHtml(context?.categoryName || `票档 ${ticket.item.categoryId}`)}</li>
                <li><strong>座位</strong> ${ticket.seatId ? escapeHtml(seatLabelFromContext(context, ticket.seatId)) : `系统分配 ${index + 1}`}</li>
              </ul>
            </article>
          `).join("")}
        </div>
        <div class="button-row">
          <a class="btn" href="#/orders">订单列表</a>
          <a class="btn" href="#/">继续购票</a>
        </div>
      </section>
    `;
  }

  async function renderOrders() {
    if (!requireLogin()) return;
    renderLoading("正在加载订单列表");
    try {
      const orders = await apiGet("/api/user/orders");
      app.innerHTML = `
        <section class="band">
          <div class="section-title">
            <div>
              <h1>我的订单</h1>
              <p>${Array.isArray(orders) ? orders.length : 0} 个订单</p>
            </div>
            <a class="btn primary" href="#/">继续购票</a>
          </div>
          ${Array.isArray(orders) && orders.length ? `
            <div class="order-list">
              ${orders.map(orderCard).join("")}
            </div>
          ` : emptyState("暂无订单", "完成选座下单后，订单会出现在这里。")}
        </section>
      `;
      bindOrderList();
    } catch (error) {
      renderError("订单列表加载失败", error, () => renderOrders());
    }
  }

  function orderCard(order) {
    const orderId = resolveOrderId(order);
    const context = readContext(orderId);
    return `
      <article class="order-card">
        <div class="order-top">
          <h3>${escapeHtml(context?.performanceTitle || `订单 ${order.orderSn || order.orderId}`)}</h3>
          ${statusPill(order.status)}
        </div>
        <ul class="meta-list">
          <li><strong>订单号</strong> ${escapeHtml(order.orderSn || order.orderId)}</li>
          <li><strong>演出时间</strong> ${context?.showTime ? formatDateTime(context.showTime) : `#${escapeHtml(order.showId)}`}</li>
          <li><strong>金额</strong> ${formatMoney(order.totalAmount)} · <strong>截止</strong> ${formatDateTime(order.payExpireTime)}</li>
        </ul>
        <div class="button-row">
          <a class="btn primary" href="${order.status === "PAID" ? ticketsHash(orderId) : checkoutHash(orderId)}">${order.status === "PAID" ? "电子票" : "查看订单"}</a>
          ${order.status === "PENDING_PAYMENT" ? `<a class="btn" href="${checkoutHash(orderId)}">去支付</a>` : ""}
          ${order.status === "PENDING_PAYMENT" ? `<button class="btn danger" type="button" data-user-order-cancel="${escapeAttr(orderId)}">取消订单</button>` : ""}
        </div>
      </article>
    `;
  }

  function bindOrderList() {
    document.querySelectorAll("[data-user-order-cancel]").forEach((button) => {
      button.addEventListener("click", async () => {
        const orderId = normalizeId(button.dataset.userOrderCancel);
        if (!window.confirm("确认取消该待支付订单？")) {
          return;
        }
        button.disabled = true;
        try {
          await apiPost("/api/user/orders/cancel", { orderId });
          toast("订单已取消", "success");
          renderOrders();
        } catch (error) {
          button.disabled = false;
          toast(apiMessage(error), "error");
        }
      });
    });
  }

  async function renderAccount(tab) {
    if (!requireLogin()) return;
    const currentTab = ["profile", "addresses", "attendees"].includes(tab) ? tab : "profile";
    renderLoading("正在加载用户中心");
    try {
      if (currentTab === "profile") {
        const user = await apiGet("/api/user/me");
        saveUser(user);
        app.innerHTML = accountShell(currentTab, profileTemplate(user));
        bindProfileForm();
        return;
      }
      if (currentTab === "addresses") {
        const addresses = await apiGet("/api/user/addresses");
        app.innerHTML = accountShell(currentTab, addressesTemplate(Array.isArray(addresses) ? addresses : []));
        bindAddresses(Array.isArray(addresses) ? addresses : []);
        return;
      }
      const attendees = await apiGet("/api/user/attendees");
      app.innerHTML = accountShell(currentTab, attendeesTemplate(Array.isArray(attendees) ? attendees : []));
      bindAttendees(Array.isArray(attendees) ? attendees : []);
    } catch (error) {
      renderError("用户中心加载失败", error, () => renderAccount(currentTab));
    }
  }

  function accountShell(tab, body) {
    return `
      <section class="band">
        <div class="section-title">
          <div>
            <h1>我的账户</h1>
            <p>${escapeHtml(state.user?.username || `用户 #${currentUserId()}`)}</p>
          </div>
          <div class="button-row">
            <a class="btn" href="#/">继续购票</a>
            <a class="btn" href="#/orders">我的订单</a>
          </div>
        </div>
        <div class="account-layout">
          <nav class="account-tabs" aria-label="账户功能">
            <a href="#/account/profile" class="${tab === "profile" ? "active" : ""}">个人资料</a>
            <a href="#/account/addresses" class="${tab === "addresses" ? "active" : ""}">收货地址</a>
            <a href="#/account/attendees" class="${tab === "attendees" ? "active" : ""}">观演人</a>
          </nav>
          <div class="account-content">${body}</div>
        </div>
      </section>
    `;
  }

  function profileTemplate(user) {
    return `
      <section class="panel account-section">
        <div class="section-title">
          <div>
            <h2>个人资料</h2>
            <p>用户名不可在当前接口中修改。</p>
          </div>
          <span class="pill ${Number(user.status) === 1 ? "ok" : "danger"}">${userStatusText(user.status)}</span>
        </div>
        <form id="profile-form" class="form-grid two-col">
          <div class="field">
            <label for="profile-user-id">用户 ID</label>
            <input id="profile-user-id" value="${escapeAttr(user.userId || "")}" disabled>
          </div>
          <div class="field">
            <label for="profile-username">用户名</label>
            <input id="profile-username" value="${escapeAttr(user.username || "")}" disabled>
          </div>
          <div class="field">
            <label for="profile-mobile">手机号</label>
            <input id="profile-mobile" name="mobile" value="${escapeAttr(user.mobile || "")}" required>
          </div>
          <div class="field">
            <label for="profile-real-name">真实姓名</label>
            <input id="profile-real-name" name="realName" value="${escapeAttr(user.realName || "")}">
          </div>
          <div class="button-row two-col-span">
            <button class="btn primary" type="submit">保存资料</button>
          </div>
        </form>
      </section>
    `;
  }

  function bindProfileForm() {
    const form = document.querySelector("#profile-form");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const button = form.querySelector("button[type='submit']");
      button.disabled = true;
      try {
        const user = await apiPut("/api/user/me", {
          mobile: textField(form, "mobile"),
          realName: textField(form, "realName")
        });
        saveUser(user);
        toast("资料已更新", "success");
        renderAccount("profile");
      } catch (error) {
        button.disabled = false;
        toast(apiMessage(error), "error");
      }
    });
  }

  function addressesTemplate(addresses) {
    return `
      <section class="panel account-section">
        <div class="section-title">
          <div>
            <h2>收货地址</h2>
            <p>${addresses.length} 个地址</p>
          </div>
          <button class="btn primary" type="button" id="add-address">新增地址</button>
        </div>
        ${addresses.length ? `
          <div class="management-list">
            ${addresses.map(addressCard).join("")}
          </div>
        ` : inlineEmpty("暂无地址", "新增地址后可在订单配送或联系场景中使用。")}
      </section>
    `;
  }

  function addressCard(address) {
    const addressId = normalizeId(address.addressId);
    return `
      <article class="management-card">
        <div class="management-top">
          <div>
            <h3>${escapeHtml(address.receiverName || "-")}</h3>
            <p>${escapeHtml(address.receiverMobile || "-")}</p>
          </div>
          ${Number(address.defaultFlag) === 1 ? `<span class="pill ok">默认</span>` : `<span class="pill">普通</span>`}
        </div>
        <p>${escapeHtml([address.province, address.city, address.district, address.detailAddress].filter(Boolean).join(" "))}</p>
        <p class="muted">邮编 ${escapeHtml(address.postalCode || "-")}</p>
        <div class="button-row">
          <button class="btn compact" type="button" data-edit-address="${escapeAttr(addressId)}">编辑</button>
          ${Number(address.defaultFlag) === 1 ? "" : `<button class="btn compact" type="button" data-default-address="${escapeAttr(addressId)}">设为默认</button>`}
          <button class="btn danger compact" type="button" data-delete-address="${escapeAttr(addressId)}">删除</button>
        </div>
      </article>
    `;
  }

  function bindAddresses(addresses) {
    const byId = new Map(addresses.map((item) => [normalizeId(item.addressId), item]));
    document.querySelector("#add-address").addEventListener("click", () => openAddressDialog());
    document.querySelectorAll("[data-edit-address]").forEach((button) => {
      button.addEventListener("click", async () => {
        const addressId = normalizeId(button.dataset.editAddress);
        button.disabled = true;
        try {
          const address = await apiGet(`/api/user/addresses/${encodeURIComponent(addressId)}`);
          openAddressDialog(address || byId.get(addressId));
        } catch (error) {
          toast(apiMessage(error), "error");
        } finally {
          button.disabled = false;
        }
      });
    });
    document.querySelectorAll("[data-default-address]").forEach((button) => {
      button.addEventListener("click", () => setDefaultAddress(button));
    });
    document.querySelectorAll("[data-delete-address]").forEach((button) => {
      button.addEventListener("click", () => deleteAddress(button));
    });
  }

  async function setDefaultAddress(button) {
    button.disabled = true;
    try {
      await apiPut(`/api/user/addresses/${encodeURIComponent(normalizeId(button.dataset.defaultAddress))}/default`);
      toast("默认地址已更新", "success");
      renderAccount("addresses");
    } catch (error) {
      button.disabled = false;
      toast(apiMessage(error), "error");
    }
  }

  async function deleteAddress(button) {
    if (!window.confirm("确认删除该地址？")) {
      return;
    }
    button.disabled = true;
    try {
      await apiDelete(`/api/user/addresses/${encodeURIComponent(normalizeId(button.dataset.deleteAddress))}`);
      toast("地址已删除", "success");
      renderAccount("addresses");
    } catch (error) {
      button.disabled = false;
      toast(apiMessage(error), "error");
    }
  }

  function openAddressDialog(address) {
    const editing = Boolean(address);
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog wide" role="dialog" aria-modal="true" aria-labelledby="address-title">
          <div class="dialog-head">
            <h2 id="address-title">${editing ? "编辑地址" : "新增地址"}</h2>
            <button class="btn compact" type="button" data-action="close-dialog">关闭</button>
          </div>
          <div class="dialog-body">
            <form id="address-form" class="form-grid two-col">
              <div class="field">
                <label for="address-receiver-name">收件人</label>
                <input id="address-receiver-name" name="receiverName" value="${escapeAttr(address?.receiverName || "")}" maxlength="64" required>
              </div>
              <div class="field">
                <label for="address-receiver-mobile">手机号</label>
                <input id="address-receiver-mobile" name="receiverMobile" value="${escapeAttr(address?.receiverMobile || "")}" maxlength="32" required>
              </div>
              <div class="field">
                <label for="address-province">省份</label>
                <input id="address-province" name="province" value="${escapeAttr(address?.province || "")}" maxlength="64" required>
              </div>
              <div class="field">
                <label for="address-city">城市</label>
                <input id="address-city" name="city" value="${escapeAttr(address?.city || "")}" maxlength="64" required>
              </div>
              <div class="field">
                <label for="address-district">区县</label>
                <input id="address-district" name="district" value="${escapeAttr(address?.district || "")}" maxlength="64">
              </div>
              <div class="field">
                <label for="address-postal-code">邮编</label>
                <input id="address-postal-code" name="postalCode" value="${escapeAttr(address?.postalCode || "")}" maxlength="16">
              </div>
              <div class="field two-col-span">
                <label for="address-detail">详细地址</label>
                <input id="address-detail" name="detailAddress" value="${escapeAttr(address?.detailAddress || "")}" maxlength="255" required>
              </div>
              <label class="check-row two-col-span">
                <input type="checkbox" name="defaultFlag" ${Number(address?.defaultFlag) === 1 ? "checked" : ""}>
                设为默认地址
              </label>
              <div class="button-row two-col-span">
                <button class="btn primary" type="submit">${editing ? "保存地址" : "创建地址"}</button>
              </div>
            </form>
          </div>
        </section>
      </div>
    `;
    const form = dialogRoot.querySelector("#address-form");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const button = form.querySelector("button[type='submit']");
      button.disabled = true;
      try {
        const payload = {
          receiverName: textField(form, "receiverName"),
          receiverMobile: textField(form, "receiverMobile"),
          province: textField(form, "province"),
          city: textField(form, "city"),
          district: textField(form, "district"),
          detailAddress: textField(form, "detailAddress"),
          postalCode: textField(form, "postalCode"),
          defaultFlag: form.elements.defaultFlag.checked ? 1 : 0
        };
        if (editing) {
          payload.addressId = normalizeId(address.addressId);
          await apiPut("/api/user/addresses", payload);
        } else {
          await apiPost("/api/user/addresses", payload);
        }
        closeDialog();
        toast(editing ? "地址已更新" : "地址已创建", "success");
        renderAccount("addresses");
      } catch (error) {
        button.disabled = false;
        toast(apiMessage(error), "error");
      }
    });
    dialogRoot.querySelector("input").focus();
  }

  function attendeesTemplate(attendees) {
    return `
      <section class="panel account-section">
        <div class="section-title">
          <div>
            <h2>观演人</h2>
            <p>${attendees.length} 个实名观演人</p>
          </div>
          <button class="btn primary" type="button" id="add-attendee">新增观演人</button>
        </div>
        ${attendees.length ? `
          <div class="management-list">
            ${attendees.map(attendeeCard).join("")}
          </div>
        ` : inlineEmpty("暂无观演人", "实名制项目下单前可先维护常用观演人。")}
      </section>
    `;
  }

  function attendeeCard(attendee) {
    const attendeeId = normalizeId(attendee.attendeeId);
    return `
      <article class="management-card">
        <div class="management-top">
          <div>
            <h3>${escapeHtml(attendee.realName || "-")}</h3>
            <p>${escapeHtml(attendee.mobile || "-")}</p>
          </div>
          ${Number(attendee.defaultFlag) === 1 ? `<span class="pill ok">默认</span>` : `<span class="pill">普通</span>`}
        </div>
        <p>${certificateTypeText(attendee.certificateType)} · ${escapeHtml(attendee.certificateNo || "-")}</p>
        <p class="muted">状态 ${userStatusText(attendee.status)}</p>
        <div class="button-row">
          <button class="btn compact" type="button" data-edit-attendee="${escapeAttr(attendeeId)}">编辑</button>
          ${Number(attendee.defaultFlag) === 1 ? "" : `<button class="btn compact" type="button" data-default-attendee="${escapeAttr(attendeeId)}">设为默认</button>`}
          <button class="btn danger compact" type="button" data-delete-attendee="${escapeAttr(attendeeId)}">删除</button>
        </div>
      </article>
    `;
  }

  function bindAttendees(attendees) {
    const byId = new Map(attendees.map((item) => [normalizeId(item.attendeeId), item]));
    document.querySelector("#add-attendee").addEventListener("click", () => openAttendeeDialog());
    document.querySelectorAll("[data-edit-attendee]").forEach((button) => {
      button.addEventListener("click", async () => {
        const attendeeId = normalizeId(button.dataset.editAttendee);
        button.disabled = true;
        try {
          const attendee = await apiGet(`/api/user/attendees/${encodeURIComponent(attendeeId)}`);
          openAttendeeDialog(attendee || byId.get(attendeeId));
        } catch (error) {
          toast(apiMessage(error), "error");
        } finally {
          button.disabled = false;
        }
      });
    });
    document.querySelectorAll("[data-default-attendee]").forEach((button) => {
      button.addEventListener("click", () => setDefaultAttendee(button));
    });
    document.querySelectorAll("[data-delete-attendee]").forEach((button) => {
      button.addEventListener("click", () => deleteAttendee(button));
    });
  }

  async function setDefaultAttendee(button) {
    button.disabled = true;
    try {
      await apiPut(`/api/user/attendees/${encodeURIComponent(normalizeId(button.dataset.defaultAttendee))}/default`);
      toast("默认观演人已更新", "success");
      renderAccount("attendees");
    } catch (error) {
      button.disabled = false;
      toast(apiMessage(error), "error");
    }
  }

  async function deleteAttendee(button) {
    if (!window.confirm("确认删除该观演人？")) {
      return;
    }
    button.disabled = true;
    try {
      await apiDelete(`/api/user/attendees/${encodeURIComponent(normalizeId(button.dataset.deleteAttendee))}`);
      toast("观演人已删除", "success");
      renderAccount("attendees");
    } catch (error) {
      button.disabled = false;
      toast(apiMessage(error), "error");
    }
  }

  function openAttendeeDialog(attendee) {
    const editing = Boolean(attendee);
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog wide" role="dialog" aria-modal="true" aria-labelledby="attendee-title">
          <div class="dialog-head">
            <h2 id="attendee-title">${editing ? "编辑观演人" : "新增观演人"}</h2>
            <button class="btn compact" type="button" data-action="close-dialog">关闭</button>
          </div>
          <div class="dialog-body">
            <form id="attendee-form" class="form-grid two-col">
              <div class="field">
                <label for="attendee-real-name">真实姓名</label>
                <input id="attendee-real-name" name="realName" value="${escapeAttr(attendee?.realName || "")}" required>
              </div>
              <div class="field">
                <label for="attendee-mobile">手机号</label>
                <input id="attendee-mobile" name="mobile" value="${escapeAttr(attendee?.mobile || "")}" required>
              </div>
              <div class="field">
                <label for="attendee-certificate-type">证件类型</label>
                <select id="attendee-certificate-type" name="certificateType" required>
                  ${option("ID_CARD", "身份证", attendee?.certificateType || "ID_CARD")}
                  ${option("PASSPORT", "护照", attendee?.certificateType || "ID_CARD")}
                </select>
              </div>
              <div class="field">
                <label for="attendee-certificate-no">证件号码</label>
                <input id="attendee-certificate-no" name="certificateNo" value="${escapeAttr(attendee?.certificateNo || "")}" required>
              </div>
              <label class="check-row two-col-span">
                <input type="checkbox" name="defaultFlag" ${Number(attendee?.defaultFlag) === 1 ? "checked" : ""}>
                设为默认观演人
              </label>
              <div class="button-row two-col-span">
                <button class="btn primary" type="submit">${editing ? "保存观演人" : "创建观演人"}</button>
              </div>
            </form>
          </div>
        </section>
      </div>
    `;
    const form = dialogRoot.querySelector("#attendee-form");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const button = form.querySelector("button[type='submit']");
      button.disabled = true;
      try {
        const payload = {
          realName: textField(form, "realName"),
          certificateType: textField(form, "certificateType"),
          certificateNo: textField(form, "certificateNo"),
          mobile: textField(form, "mobile"),
          defaultFlag: form.elements.defaultFlag.checked ? 1 : 0
        };
        if (editing) {
          payload.attendeeId = normalizeId(attendee.attendeeId);
          await apiPut("/api/user/attendees", payload);
        } else {
          await apiPost("/api/user/attendees", payload);
        }
        closeDialog();
        toast(editing ? "观演人已更新" : "观演人已创建", "success");
        renderAccount("attendees");
      } catch (error) {
        button.disabled = false;
        toast(apiMessage(error), "error");
      }
    });
    dialogRoot.querySelector("input").focus();
  }

  function renderAuth() {
    const userId = currentUserId();
    const label = state.user?.username || (userId ? `用户 #${userId}` : "未登录");
    authRoot.innerHTML = `
      <div class="auth-inline">
        ${state.token ? `
          <a class="btn compact" href="#/account">${escapeHtml(label)}</a>
          <button class="btn ghost compact" type="button" data-action="logout">退出</button>
        ` : `
          <button class="btn compact" type="button" data-action="open-login">登录</button>
          <button class="btn compact" type="button" data-action="open-register">注册</button>
        `}
      </div>
    `;
  }

  function openAuthDialog(mode) {
    const login = mode === "login";
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="auth-title">
          <div class="dialog-head">
            <h2 id="auth-title">${login ? "登录" : "注册"}</h2>
            <button class="btn compact" type="button" data-action="close-dialog">关闭</button>
          </div>
          <div class="dialog-body">
            <form id="${login ? "login-form" : "register-form"}" class="form-grid">
              <div class="field">
                <label for="auth-username">用户名</label>
                <input id="auth-username" name="username" required autocomplete="username">
              </div>
              <div class="field">
                <label for="auth-password">密码</label>
                <input id="auth-password" name="password" type="password" required autocomplete="${login ? "current-password" : "new-password"}">
              </div>
              ${login ? "" : `
                <div class="field">
                  <label for="auth-mobile">手机号</label>
                  <input id="auth-mobile" name="mobile" required>
                </div>
                <div class="field">
                  <label for="auth-real-name">姓名</label>
                  <input id="auth-real-name" name="realName">
                </div>
              `}
              <button class="btn primary" type="submit">${login ? "登录" : "注册并使用"}</button>
            </form>
          </div>
        </section>
      </div>
    `;
    const form = dialogRoot.querySelector("form");
    form.addEventListener("submit", login ? submitLogin : submitRegister);
    dialogRoot.querySelector("input").focus();
  }

  async function submitLogin(event) {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(event.target).entries());
    try {
      const result = await apiPost("/api/user/login", payload, { auth: false });
      setAuth(result.accessToken, result.user);
      closeDialog();
      toast("登录成功", "success");
      route();
    } catch (error) {
      toast(apiMessage(error), "error");
    }
  }

  async function submitRegister(event) {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(event.target).entries());
    try {
      const user = await apiPost("/api/user/register", payload, { auth: false });
      setAuth(`dev-${user.userId}`, user);
      closeDialog();
      toast("注册成功", "success");
      route();
    } catch (error) {
      toast(apiMessage(error), "error");
    }
  }

  async function refreshUser() {
    if (!state.token) {
      saveUser(null);
      renderAuth();
      return;
    }
    try {
      const user = await apiGet("/api/user/me");
      saveUser(user);
      renderAuth();
    } catch (_error) {
      saveUser(null);
      renderAuth();
    }
  }

  function setAuth(token, user, options) {
    state.token = token || "";
    state.user = user || null;
    localStorage.removeItem(STORAGE_TOKEN);
    localStorage.removeItem(STORAGE_USER);
    if (!user && options?.refresh) {
      refreshUser();
    }
    renderAuth();
  }

  async function logout() {
    const hadToken = Boolean(state.token);
    try {
      if (hadToken) {
        await apiPost("/api/user/logout");
      }
    } catch (error) {
      toast(apiMessage(error), "error");
    } finally {
      setAuth("", null);
      toast("已退出登录", "success");
      const { path } = parseHash();
      if (isProtectedRoute(path)) {
        window.location.hash = "#/";
      }
    }
  }

  function closeDialog() {
    dialogRoot.innerHTML = "";
  }

  function setActionButtonsDisabled(disabled) {
    document.querySelectorAll("[data-create-order], [data-pay-create], [data-pay-success], [data-order-cancel]").forEach((button) => {
      button.disabled = disabled;
    });
  }

  async function apiGet(path, options) {
    return request(path, { method: "GET", ...(options || {}) });
  }

  async function apiPost(path, body, options) {
    return request(path, { method: "POST", body, ...(options || {}) });
  }

  async function apiPut(path, body, options) {
    return request(path, { method: "PUT", body, ...(options || {}) });
  }

  async function apiDelete(path, options) {
    return request(path, { method: "DELETE", ...(options || {}) });
  }

  async function request(path, options) {
    const headers = {
      Accept: "application/json"
    };
    if (options.body !== undefined) {
      headers["Content-Type"] = "application/json";
    }
    if (options.auth !== false && state.token) {
      headers.Authorization = `${AUTHORIZATION_PREFIX}${state.token}`;
    }
    const userId = currentUserId();
    if (options.auth !== false && userId) {
      headers[USER_ID_HEADER] = userId;
    }
    let response;
    try {
      response = await fetch(apiUrl(path), {
        method: options.method || "GET",
        headers,
        body: options.body !== undefined ? JSON.stringify(options.body) : undefined
      });
    } catch (error) {
      throw new Error("无法连接到网关或后端服务");
    }
    const payload = await parseResponsePayload(response);
    if (!response.ok) {
      const message = payload?.message || `${response.status} ${response.statusText}`;
      throw new Error(withRequestId(message, payload?.requestId));
    }
    if (payload && Object.prototype.hasOwnProperty.call(payload, "code")) {
      if (payload.code !== SUCCESS_CODE) {
        throw new Error(withRequestId(payload.message || "请求失败", payload.requestId));
      }
      return payload.data;
    }
    return payload;
  }

  async function parseResponsePayload(response) {
    const text = await response.text().catch(() => "");
    if (!text) {
      return null;
    }
    try {
      return JSON.parse(quoteUnsafeIntegerIds(text));
    } catch (_error) {
      return null;
    }
  }

  function quoteUnsafeIntegerIds(text) {
    return text.replace(/"((?:order|pay|lock|user|refund|address|attendee|show|category|item|seat)Id)"\s*:\s*(\d{16,})/g, '"$1":"$2"');
  }

  function apiUrl(path) {
    if (/^https?:\/\//i.test(path)) {
      return path;
    }
    return `${API_BASE}${path.startsWith("/") ? path : `/${path}`}`;
  }

  function normalizeBaseUrl(value) {
    return String(value || "").replace(/\/+$/, "");
  }

  function currentUserId() {
    return normalizeId(state.user?.userId) || parseDevUserId(state.token);
  }

  function saveUser(user) {
    state.user = user;
    localStorage.removeItem(STORAGE_USER);
  }

  function requireLogin() {
    if (state.token && currentUserId()) {
      return true;
    }
    app.innerHTML = loginRequiredTemplate();
    renderAuth();
    document.querySelector("[data-login-required]")?.addEventListener("click", () => openAuthDialog("login"));
    return false;
  }

  function loginRequiredTemplate() {
    return `
      <section class="state-box">
        <h2>请先登录</h2>
        <p>用户资料、订单、地址和观演人管理需要先登录。</p>
        <div class="button-row">
          <button class="btn primary" type="button" data-login-required>登录</button>
          <button class="btn" type="button" data-action="open-register">注册</button>
        </div>
      </section>
    `;
  }

  function isProtectedRoute(path) {
    return path === "/orders" || path.startsWith("/checkout/") || path.startsWith("/tickets/") || path === "/account" || path.startsWith("/account/");
  }

  function ensureSelection(showId, availability) {
    const key = String(showId);
    if (!state.detailSelection[key]) {
      const firstAvailable = availability.find((item) => Number(item.availableStock) > 0) || availability[0];
      state.detailSelection[key] = {
        categoryId: firstAvailable?.categoryId || null,
        selectedSeats: [],
        quantity: 1
      };
    }
    const selection = state.detailSelection[key];
    const categoryExists = availability.some((item) => Number(item.categoryId) === Number(selection.categoryId));
    if (!categoryExists) {
      selection.categoryId = availability[0]?.categoryId || null;
      selection.selectedSeats = [];
      selection.quantity = 1;
    }
    return selection;
  }

  function selectedSeatDetails(seats, seatIds) {
    const map = new Map((seats || []).map((seat) => [Number(seat.seatId), seat]));
    return (seatIds || []).map((id) => map.get(Number(id))).filter(Boolean);
  }

  function rememberContext(orderId, context) {
    const contexts = readJson(STORAGE_CONTEXT) || {};
    contexts[normalizeId(orderId)] = context;
    localStorage.setItem(STORAGE_CONTEXT, JSON.stringify(contexts));
  }

  function readContext(orderId) {
    const contexts = readJson(STORAGE_CONTEXT) || {};
    return contexts[normalizeId(orderId)] || null;
  }

  function resolveOrderId(order, fallback) {
    return idFromSerial(order?.orderSn, "EO") || normalizeId(order?.orderId) || normalizeId(fallback);
  }

  function resolvePayId(pay, fallback) {
    return idFromSerial(pay?.paySn, "EP") || normalizeId(pay?.payId) || normalizeId(fallback);
  }

  function idFromSerial(value, prefix) {
    const text = normalizeId(value);
    if (!text.startsWith(prefix)) {
      return "";
    }
    const id = text.slice(prefix.length);
    return /^\d+$/.test(id) ? id : "";
  }

  function normalizeId(value) {
    if (value === null || value === undefined) {
      return "";
    }
    if (typeof value === "bigint") {
      return value.toString();
    }
    if (typeof value === "number") {
      return Number.isFinite(value) ? value.toFixed(0) : "";
    }
    return String(value).trim();
  }

  function checkoutHash(orderId) {
    return `#/checkout/${encodeURIComponent(normalizeId(orderId))}`;
  }

  function ticketsHash(orderId) {
    return `#/tickets/${encodeURIComponent(normalizeId(orderId))}`;
  }

  function seatLabelFromContext(context, seatId) {
    const seat = context?.seats?.find((item) => Number(item.seatId) === Number(seatId));
    return seat?.seatNo || `#${seatId}`;
  }

  function renderLoading(title) {
    app.innerHTML = `
      <section class="loading panel">
        <h1>${escapeHtml(title)}</h1>
        <div class="skeleton" style="width: 80%"></div>
        <div class="skeleton" style="width: 64%"></div>
        <div class="skeleton" style="width: 92%"></div>
      </section>
    `;
  }

  function renderError(title, error, retry) {
    app.innerHTML = `
      <section class="state-box">
        <h2>${escapeHtml(title)}</h2>
        <p>${escapeHtml(apiMessage(error))}</p>
        <div class="button-row">
          <button class="btn primary" type="button" id="retry">重试</button>
          <a class="btn" href="#/">返回项目</a>
        </div>
      </section>
    `;
    document.querySelector("#retry").addEventListener("click", retry);
  }

  function renderNotFound() {
    app.innerHTML = emptyState("页面不存在", "请从项目列表或订单列表继续。");
  }

  function emptyState(title, text) {
    return `
      <section class="state-box">
        <h2>${escapeHtml(title)}</h2>
        <p>${escapeHtml(text)}</p>
      </section>
    `;
  }

  function inlineEmpty(title, text) {
    return `
      <div class="inline-empty">
        <h3>${escapeHtml(title)}</h3>
        <p>${escapeHtml(text)}</p>
      </div>
    `;
  }

  function toast(message, type) {
    const node = document.createElement("div");
    node.className = `toast ${type || ""}`;
    node.textContent = message;
    toastRoot.appendChild(node);
    setTimeout(() => node.remove(), 4200);
  }

  function parseHash() {
    const hash = window.location.hash.replace(/^#/, "") || "/";
    const [path, search = ""] = hash.split("?");
    return {
      path: path.startsWith("/") ? path : `/${path}`,
      query: Object.fromEntries(new URLSearchParams(search).entries())
    };
  }

  function updateNav(path) {
    document.querySelectorAll("[data-nav]").forEach((item) => item.classList.remove("active"));
    if (path === "/orders" || path.startsWith("/checkout/") || path.startsWith("/tickets/")) {
      document.querySelector('[data-nav="orders"]')?.classList.add("active");
    } else if (path === "/account" || path.startsWith("/account/")) {
      document.querySelector('[data-nav="account"]')?.classList.add("active");
    } else {
      document.querySelector('[data-nav="home"]')?.classList.add("active");
    }
  }

  function option(value, label, selected) {
    return `<option value="${value}" ${value === selected ? "selected" : ""}>${label}</option>`;
  }

  function posterFor(item) {
    const title = (item.title || "").toLowerCase();
    if (title.includes("aurora")) {
      return "/assets/posters/aurora-live.png";
    }
    if (title.includes("night train")) {
      return "/assets/posters/night-train.png";
    }
    return item.posterUrl || "/assets/posters/generic.png";
  }

  function typeText(type) {
    const map = {
      CONCERT: "演唱会",
      DRAMA: "戏剧",
      MOVIE: "电影"
    };
    return map[type] || type || "项目";
  }

  function statusText(status) {
    const map = {
      AVAILABLE: "可选",
      LOCKED: "已锁定",
      SOLD: "已售",
      PENDING_PAYMENT: "待支付",
      PAID: "已支付",
      CANCELED: "已取消",
      CLOSED: "已关闭",
      WAITING: "待支付",
      SUCCESS: "成功"
    };
    return map[status] || status || "-";
  }

  function userStatusText(status) {
    const value = Number(status);
    if (value === 1) return "启用";
    if (value === 0) return "禁用";
    return status || "-";
  }

  function certificateTypeText(type) {
    const map = {
      ID_CARD: "身份证",
      PASSPORT: "护照"
    };
    return map[type] || type || "-";
  }

  function statusPill(status) {
    const cls = status === "PAID" || status === "SUCCESS" ? "ok" : status === "PENDING_PAYMENT" || status === "WAITING" ? "warn" : "danger";
    return `<span class="pill ${cls}">${escapeHtml(statusText(status))}</span>`;
  }

  function formatVenueAddress(venue) {
    if (!venue) {
      return "";
    }
    if (venue.address) {
      return venue.address;
    }
    const parts = [];
    [
      venue.province,
      venue.city,
      venue.district,
      venue.town,
      venue.village,
      venue.street,
      venue.houseNumber,
      venue.estate,
      venue.building
    ].forEach((value) => appendAddressPart(parts, value));
    return parts.join("");
  }

  function appendAddressPart(parts, value) {
    const text = String(value || "").trim();
    if (!text || parts[parts.length - 1] === text) {
      return;
    }
    parts.push(text);
  }

  function formatDateTime(value) {
    if (!value) return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return String(value).replace("T", " ");
    }
    return new Intl.DateTimeFormat("zh-CN", {
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit"
    }).format(date);
  }

  function formatMoney(value) {
    const amount = Number(value || 0);
    return new Intl.NumberFormat("zh-CN", {
      style: "currency",
      currency: "CNY"
    }).format(amount);
  }

  function shortSeatNo(value) {
    const text = String(value || "");
    return text.length > 3 ? text.slice(-3) : text;
  }

  function parseDevUserId(token) {
    const match = /^dev-(\d+)$/.exec(token || "");
    return match ? match[1] : "";
  }

  function readJson(key) {
    try {
      const text = localStorage.getItem(key);
      return text ? JSON.parse(text) : null;
    } catch (_error) {
      return null;
    }
  }

  function textField(form, name) {
    return String(new FormData(form).get(name) || "").trim();
  }

  function clamp(value, min, max) {
    return Math.max(min, Math.min(max, Number.isFinite(value) ? value : min));
  }

  function lockErrorMessage(error) {
    const message = apiMessage(error);
    if (/Seat is not available|Insufficient ticket stock|locked|sold|available/i.test(message)) {
      return "座位或库存已变化，请刷新后重新选择。";
    }
    return message;
  }

  function apiMessage(error) {
    return error?.message || "请求失败";
  }

  function withRequestId(message, requestId) {
    return requestId ? `${message}（${requestId}）` : message;
  }

  function escapeHtml(value) {
    return String(value ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  }

  function escapeAttr(value) {
    return escapeHtml(value);
  }
})();
