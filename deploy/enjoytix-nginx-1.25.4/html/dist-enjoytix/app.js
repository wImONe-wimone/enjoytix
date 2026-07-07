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
    adminOptions: {
      artists: null,
      venues: null
    },
    adminSeatManager: null,
    adminTicketEditor: null,
    adminShowTicketDraft: null,
    performancePreview: {
      cache: new Map(),
      activeId: "",
      requestSeq: 0,
      hideTimer: null
    },
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
    if (name === "open-refund-rollback") {
      openRefundRollbackDialog(action.dataset.orderId);
    }
    if (name === "open-order-refund") {
      openOrderRefundDialog(action.dataset.orderId);
    }
    if (name === "open-admin-performance-detail") {
      openAdminPerformanceDetail(action.dataset.performanceId);
    }
    if (name === "open-admin-performance-form") {
      openAdminPerformanceForm(action.dataset.performanceId);
    }
    if (name === "delete-admin-performance") {
      deleteAdminPerformance(action.dataset.performanceId);
    }
    if (name === "start-admin-performance-sale") {
      startAdminPerformanceSale(action.dataset.performanceId);
    }
    if (name === "open-admin-performance-schedule-sale") {
      openAdminPerformanceScheduleSale(action.dataset.performanceId);
    }
    if (name === "open-admin-artist-form") {
      openAdminArtistForm(action.dataset.artistId);
    }
    if (name === "delete-admin-artist") {
      deleteAdminArtist(action.dataset.artistId);
    }
    if (name === "open-admin-venue-form") {
      openAdminVenueForm(action.dataset.venueId);
    }
    if (name === "delete-admin-venue") {
      deleteAdminVenue(action.dataset.venueId);
    }
    if (name === "open-admin-venue-seats") {
      openAdminVenueSeats(action.dataset.venueId);
    }
    if (name === "open-admin-venue-seat-form") {
      openAdminVenueSeatForm(action.dataset.venueId, action.dataset.seatId);
    }
    if (name === "delete-admin-venue-seat") {
      deleteAdminVenueSeat(action.dataset.venueId, action.dataset.seatId);
    }
    if (name === "open-admin-show-form") {
      openAdminShowForm(action.dataset.performanceId, action.dataset.showId);
    }
    if (name === "delete-admin-show") {
      deleteAdminShow(action.dataset.performanceId, action.dataset.showId);
    }
    if (name === "open-admin-ticket-config") {
      openAdminTicketConfig(action.dataset.performanceId, action.dataset.showId);
    }
  }

  function route() {
    const { path, query } = parseHash();
    hidePerformancePreview(true);
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
    if (isAdminArtistRoute(path)) {
      renderAdminArtists(query);
      return;
    }
    if (isAdminVenueRoute(path)) {
      renderAdminVenues(query);
      return;
    }
    if (isAdminPerformanceRoute(path)) {
      renderAdminPerformances(query);
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
              ${performanceTypeOptions(query.performanceType)}
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
    const onSale = isOnSale(item);
    const performanceId = normalizeId(item.performanceId);
    return `
      <article class="performance-card" data-performance-preview data-performance-id="${escapeAttr(performanceId)}" data-preview-title="${escapeAttr(item.title || "")}" data-preview-type="${escapeAttr(typeText(item.performanceType))}" data-preview-sale="${escapeAttr(saleStatusText(item.saleStatus))}" data-preview-time="${escapeAttr(formatDateTime(item.earliestShowTime))}">
        <a class="poster" href="#/performance/${performanceId}" aria-label="${escapeAttr(item.title)}">
          <img src="${posterFor(item)}" alt="${escapeAttr(item.title)}海报" loading="lazy" onerror="this.onerror=null;this.src='/assets/posters/generic.png';">
          <span class="poster-badge pill ok">${typeText(item.performanceType)}</span>
        </a>
        <div class="performance-body">
          <h3><a href="#/performance/${performanceId}">${escapeHtml(item.title)}</a></h3>
          <ul class="meta-list">
            <li><strong>${escapeHtml(venue.name || "-")}</strong></li>
            <li>${escapeHtml(venueAddress)}</li>
            <li>${escapeHtml(item.artist?.name || "-")}</li>
            <li>${formatDateTime(item.earliestShowTime)}</li>
          </ul>
          <div class="button-row">
            ${saleStatusPill(item.saleStatus)}
            <a class="btn ${onSale ? "primary" : ""}" href="#/performance/${performanceId}">${onSale ? "选购门票" : "查看详情"}</a>
          </div>
        </div>
      </article>
    `;
  }

  function performancePreSaleTemplate(detail, sessions) {
    const venueAddress = formatVenueAddress(detail.venue) || "-";
    return `
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
                ${saleStatusPill(detail.saleStatus)}
              </div>
              <h1>${escapeHtml(detail.title)}</h1>
              <p>${escapeHtml(detail.description || "")}</p>
              <ul class="meta-list">
                <li><strong>${escapeHtml(detail.venue?.name || "-")}</strong> · ${escapeHtml(venueAddress)}</li>
                <li>${escapeHtml(detail.artist?.name || "-")}</li>
                <li>定时开售 ${formatDateTime(detail.scheduledSaleTime)}</li>
              </ul>
            </div>
          </div>
          <section class="panel">
            <h2>演出时间</h2>
            ${sessions.length ? `
              <div class="segmented">
                ${sessions.map((item) => `
                  <button type="button" disabled>
                    <strong>${formatDateTime(item.showTime)}</strong><br>
                    <span class="muted">时长 ${formatDuration(item.durationMinutes)} · 停售 ${formatDateTime(item.saleEndTime)}</span>
                  </button>
                `).join("")}
              </div>
            ` : inlineEmpty("暂无场次", "该项目还没有开放可展示的演出场次。")}
          </section>
        </div>
        <aside class="panel summary-panel">
          <div class="state-box">
            <h2>${escapeHtml(saleStatusText(detail.saleStatus))}</h2>
            <p class="muted">${escapeHtml(saleStatusDescription(detail))}</p>
          </div>
        </aside>
      </section>
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
    bindPerformancePreviews();
  }

  function bindPerformancePreviews() {
    document.querySelectorAll("[data-performance-preview]").forEach((card) => {
      card.addEventListener("mouseenter", () => openPerformancePreview(card));
      card.addEventListener("mousemove", () => positionPerformancePreview(card));
      card.addEventListener("mouseleave", () => hidePerformancePreview());
      card.addEventListener("focusin", () => openPerformancePreview(card));
      card.addEventListener("focusout", (event) => {
        if (!card.contains(event.relatedTarget)) {
          hidePerformancePreview();
        }
      });
    });
  }

  function ensurePerformancePreview() {
    let node = document.querySelector("[data-performance-hover-preview]");
    if (node) {
      return node;
    }
    node = document.createElement("aside");
    node.className = "performance-hover-preview";
    node.dataset.performanceHoverPreview = "";
    node.setAttribute("aria-hidden", "true");
    document.body.appendChild(node);
    return node;
  }

  async function openPerformancePreview(card) {
    const performanceId = normalizeId(card?.dataset?.performanceId);
    if (!performanceId || !/^\d+$/.test(performanceId)) {
      return;
    }
    const preview = state.performancePreview;
    window.clearTimeout(preview.hideTimer);
    preview.activeId = performanceId;
    const node = ensurePerformancePreview();
    node.innerHTML = performancePreviewLoadingTemplate(card);
    node.classList.add("visible");
    node.setAttribute("aria-hidden", "false");
    positionPerformancePreview(card);
    if (preview.cache.has(performanceId)) {
      node.innerHTML = performancePreviewTemplate(preview.cache.get(performanceId));
      positionPerformancePreview(card);
      return;
    }
    const requestSeq = ++preview.requestSeq;
    try {
      const detail = await apiGet(`/api/performance/${encodeURIComponent(performanceId)}`);
      preview.cache.set(performanceId, detail);
      if (preview.activeId !== performanceId || requestSeq !== preview.requestSeq) {
        return;
      }
      node.innerHTML = performancePreviewTemplate(detail);
      positionPerformancePreview(card);
    } catch (error) {
      if (preview.activeId !== performanceId) {
        return;
      }
      node.innerHTML = `
        <div class="preview-head">
          <strong>${escapeHtml(card?.dataset?.previewTitle || "项目预览")}</strong>
          <span class="pill danger">加载失败</span>
        </div>
        <p class="muted">${escapeHtml(apiMessage(error))}</p>
      `;
      positionPerformancePreview(card);
    }
  }

  function hidePerformancePreview(immediate = false) {
    const preview = state.performancePreview;
    window.clearTimeout(preview.hideTimer);
    const node = document.querySelector("[data-performance-hover-preview]");
    if (!node) {
      return;
    }
    const hide = () => {
      preview.activeId = "";
      node.classList.remove("visible");
      node.setAttribute("aria-hidden", "true");
    };
    if (immediate) {
      hide();
      return;
    }
    preview.hideTimer = window.setTimeout(hide, 80);
  }

  function positionPerformancePreview(card) {
    const node = document.querySelector("[data-performance-hover-preview]");
    if (!node || !card || !node.classList.contains("visible")) {
      return;
    }
    const rect = card.getBoundingClientRect();
    const gap = 12;
    const margin = 12;
    const width = node.offsetWidth || 320;
    const height = node.offsetHeight || 220;
    let left;
    if (window.innerWidth - rect.right >= width + gap + margin) {
      left = rect.right + gap;
    } else if (rect.left >= width + gap + margin) {
      left = rect.left - width - gap;
    } else {
      left = Math.min(Math.max(rect.left, margin), Math.max(margin, window.innerWidth - width - margin));
    }
    let top = rect.top;
    if (left >= rect.left && left <= rect.right && window.innerHeight - rect.bottom >= height + gap + margin) {
      top = rect.bottom + gap;
    }
    top = Math.min(Math.max(top, margin), Math.max(margin, window.innerHeight - height - margin));
    node.style.left = `${Math.round(left)}px`;
    node.style.top = `${Math.round(top)}px`;
  }

  function performancePreviewLoadingTemplate(card) {
    return `
      <div class="preview-head">
        <strong>${escapeHtml(card?.dataset?.previewTitle || "项目预览")}</strong>
        <span class="pill">${escapeHtml(card?.dataset?.previewSale || "-")}</span>
      </div>
      <div class="preview-meta">
        <span>${escapeHtml(card?.dataset?.previewType || "-")}</span>
        <span>${escapeHtml(card?.dataset?.previewTime || "-")}</span>
      </div>
      <div class="preview-skeleton"></div>
      <div class="preview-skeleton short"></div>
    `;
  }

  function performancePreviewTemplate(detail) {
    const sessions = (Array.isArray(detail?.sessions) ? detail.sessions : [])
      .filter((item) => Number(item.status) === 1)
      .sort((a, b) => String(a.showTime || "").localeCompare(String(b.showTime || "")));
    const visibleSessions = sessions.slice(0, 4);
    return `
      <div class="preview-head">
        <strong>${escapeHtml(detail?.title || "项目预览")}</strong>
        ${saleStatusPill(detail?.saleStatus)}
      </div>
      <div class="preview-meta">
        <span>${escapeHtml(typeText(detail?.performanceType))}</span>
        <span>${escapeHtml(detail?.venue?.name || "-")}</span>
        <span>${escapeHtml(detail?.artist?.name || "-")}</span>
      </div>
      ${visibleSessions.length ? `
        <div class="preview-session-list">
          ${visibleSessions.map((session) => `
            <div class="preview-session">
              <span>${formatDateTime(session.showTime)}</span>
              <small>停售 ${formatDateTime(session.saleEndTime)}</small>
            </div>
          `).join("")}
        </div>
        ${sessions.length > visibleSessions.length ? `<p class="muted">还有 ${sessions.length - visibleSessions.length} 场</p>` : ""}
      ` : `
        <p class="muted">${escapeHtml(saleStatusDescription(detail))}</p>
      `}
    `;
  }

  async function renderPerformanceDetail(performanceId, query) {
    renderLoading("正在加载项目详情");
    try {
      const detail = await apiGet(`/api/performance/${encodeURIComponent(performanceId)}`);
      const activeSessions = (detail.sessions || []).filter((item) => Number(item.status) === 1);
      if (!isOnSale(detail)) {
        state.currentDetail = null;
        app.innerHTML = performancePreSaleTemplate(detail, activeSessions);
        return;
      }
      const requestedShowId = normalizeId(query.showId);
      const selectedSession = activeSessions.find((item) => normalizeId(item.showId) === requestedShowId) || activeSessions[0];
      const showId = normalizeId(selectedSession?.showId);
      if (!showId) {
        app.innerHTML = emptyState("暂无可售场次", "该项目还没有开放售票的演出场次。");
        return;
      }
      const [availability, seatMap, seats] = await Promise.all([
        apiGet(`/api/ticket/availability?showId=${encodeURIComponent(showId)}`),
        apiGet(`/api/show/${encodeURIComponent(showId)}/seat-map`),
        apiGet(`/api/ticket/seats?showId=${encodeURIComponent(showId)}`)
      ]);
      state.currentDetail = { detail: { ...detail, sessions: activeSessions }, showId, availability, seatMap, seats };
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
    const show = detail.sessions.find((item) => normalizeId(item.showId) === normalizeId(showId)) || detail.sessions[0];
    const category = availability.find((item) => sameId(item.categoryId, selection.categoryId));
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
                <button type="button" data-show-id="${escapeAttr(normalizeId(item.showId))}" class="${normalizeId(item.showId) === normalizeId(showId) ? "active" : ""}">
                  <strong>${formatDateTime(item.showTime)}</strong><br>
                  <span class="muted">时长 ${formatDuration(item.durationMinutes)} · 停售 ${formatDateTime(item.saleEndTime)}</span>
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
          <button type="button" class="category-button ${sameId(item.categoryId, selectedCategoryId) ? "active" : ""}" data-category-id="${escapeAttr(normalizeId(item.categoryId))}" ${item.availableStock <= 0 ? "disabled" : ""}>
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
    const seatState = new Map(seats.map((seat) => [normalizeId(seat.seatId), seat]));
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
    const seatId = normalizeId(seat.seatId);
    const realtime = seatState.get(seatId);
    const status = realtime?.status || "UNAVAILABLE";
    const categoryMatches = realtime && sameId(realtime.categoryId, category.categoryId);
    const selected = selection.selectedSeats.includes(seatId);
    const available = status === "AVAILABLE" && categoryMatches;
    const classNames = ["seat"];
    if (selected) classNames.push("selected");
    if (status === "LOCKED") classNames.push("locked");
    if (status === "SOLD") classNames.push("sold");
    if (!categoryMatches && status === "AVAILABLE") classNames.push("other");
    const disabled = selected ? "" : (available ? "" : "disabled");
    const label = `${seat.seatNo} ${statusText(status)}`;
    return `
      <button type="button" class="${classNames.join(" ")}" data-seat-id="${escapeAttr(seatId)}" ${disabled} title="${escapeAttr(label)}" aria-label="${escapeAttr(label)}" aria-pressed="${selected ? "true" : "false"}">
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
      <div class="summary-line"><span>演出时长</span><strong>${formatDuration(show?.durationMinutes)}</strong></div>
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
        selection.categoryId = normalizeId(button.dataset.categoryId);
        selection.selectedSeats = [];
        selection.quantity = 1;
        renderDetailFromState();
      });
    });
    document.querySelectorAll("[data-seat-id]").forEach((button) => {
      button.addEventListener("click", () => {
        const showId = state.currentDetail.showId;
        const selection = ensureSelection(showId, state.currentDetail.availability);
        const seatId = normalizeId(button.dataset.seatId);
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
    const categoryId = normalizeId(selection.categoryId);
    const seatMap = new Map(data.seats.map((seat) => [normalizeId(seat.seatId), seat]));
    selection.selectedSeats = selection.selectedSeats.filter((seatId) => {
      const seat = seatMap.get(normalizeId(seatId));
      return seat && seat.status === "AVAILABLE" && sameId(seat.categoryId, categoryId);
    });
  }

  async function createOrderFromSelection() {
    if (!state.currentDetail || state.busy) return;
    if (!requireLogin()) return;
    const data = state.currentDetail;
    const selection = ensureSelection(data.showId, data.availability);
    const category = data.availability.find((item) => sameId(item.categoryId, selection.categoryId));
    const quantity = category?.seatSelectable === 1 ? selection.selectedSeats.length : selection.quantity;
    if (!category || quantity < 1) {
      toast("请选择票档和座位", "error");
      return;
    }
    state.busy = true;
      renderDetailFromState();
    try {
      const payload = {
        showId: normalizeId(data.showId),
        categoryId: normalizeId(category.categoryId),
        quantity: Number(quantity),
        seatIds: category.seatSelectable === 1 ? selection.selectedSeats : []
      };
      const order = await apiPost("/api/order/create", payload);
      const orderId = resolveOrderId(order);
      if (!orderId) {
        throw new Error("订单创建结果缺少 order_id");
      }
      const selectedShow = data.detail.sessions.find((item) => normalizeId(item.showId) === normalizeId(data.showId)) || {};
      rememberContext(orderId, {
        performanceId: data.detail.performanceId,
        performanceTitle: data.detail.title,
        showId: data.showId,
        categoryId: normalizeId(category.categoryId),
        categoryName: category.categoryName,
        showTime: selectedShow.showTime,
        showDurationMinutes: selectedShow.durationMinutes,
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
          ${paid ? `
            <a class="btn primary" href="${ticketsHash(resolvedOrderId)}">查看电子票</a>
            <button class="btn danger" type="button" data-action="open-order-refund" data-order-id="${escapeAttr(resolvedOrderId)}" ${state.busy ? "disabled" : ""}>申请退票</button>
          ` : ""}
          ${order.status === "REFUNDING" ? `<button class="btn danger" type="button" data-action="open-refund-rollback" data-order-id="${escapeAttr(resolvedOrderId)}" ${state.busy ? "disabled" : ""}>退款回滚</button>` : ""}
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
    const unavailableState = ticketUnavailableState(order.status);
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
          ${emptyState(unavailableState.title, unavailableState.description)}
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
          <button class="btn danger" type="button" data-action="open-order-refund" data-order-id="${escapeAttr(resolvedOrderId)}" ${state.busy ? "disabled" : ""}>申请退票</button>
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
          ${order.status === "PAID" ? `<button class="btn danger" type="button" data-action="open-order-refund" data-order-id="${escapeAttr(orderId)}" ${state.busy ? "disabled" : ""}>申请退票</button>` : ""}
          ${order.status === "REFUNDING" ? `<button class="btn danger" type="button" data-action="open-refund-rollback" data-order-id="${escapeAttr(orderId)}" ${state.busy ? "disabled" : ""}>退款回滚</button>` : ""}
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

  async function renderAdminPerformances(query) {
    if (!requireLogin()) return;
    const filters = {
      title: query.title || "",
      performanceType: query.performanceType || "",
      status: query.status === "0" || query.status === "1" ? query.status : "",
      current: Number(query.current || 1),
      size: 10
    };
    renderLoading("正在加载后台项目");
    try {
      const params = new URLSearchParams();
      params.set("current", filters.current);
      params.set("size", filters.size);
      if (filters.title) params.set("title", filters.title);
      if (filters.performanceType) params.set("performanceType", filters.performanceType);
      if (filters.status) params.set("status", filters.status);
      const page = await apiGet(`/api/performance/admin/page?${params.toString()}`);
      app.innerHTML = adminPerformanceTemplate(page, filters);
      bindAdminPerformanceList(filters);
    } catch (error) {
      renderError("后台项目加载失败", error, () => renderAdminPerformances(query));
    }
  }

  function adminPerformanceTemplate(page, filters) {
    const records = Array.isArray(page.records) ? page.records : [];
    const total = Number(page.total || 0);
    const maxPage = Math.max(1, Math.ceil(total / filters.size));
    return `
      <section class="band">
        <div class="section-title">
          <div>
            <h1>演出项目管理</h1>
            <p>共 ${total} 个项目</p>
          </div>
          <button class="btn primary" type="button" data-action="open-admin-performance-form">新增项目</button>
        </div>
        ${adminTabs("performances")}
        <form id="admin-performance-filter" class="toolbar">
          <div class="field">
            <label for="admin-performance-title">项目名称</label>
            <input id="admin-performance-title" name="title" value="${escapeAttr(filters.title)}" placeholder="输入项目名称">
          </div>
          <div class="field">
            <label for="admin-performance-type">类型</label>
            <select id="admin-performance-type" name="performanceType">
              ${performanceTypeOptions(filters.performanceType)}
            </select>
          </div>
          <div class="field">
            <label for="admin-performance-status">状态</label>
            <select id="admin-performance-status" name="status">
              ${option("", "全部", filters.status)}
              ${option("1", "启用", filters.status)}
              ${option("0", "停用", filters.status)}
            </select>
          </div>
          <div class="button-row">
            <button class="btn primary" type="submit">查询</button>
            <a class="btn" href="#/admin/performances">重置</a>
          </div>
        </form>
        ${records.length ? `
          <div class="admin-table-wrap panel">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>项目</th>
                  <th>类型</th>
                  <th>艺人/团队</th>
                  <th>场馆</th>
                  <th>状态</th>
                  <th>开售状态</th>
                  <th>最早时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                ${records.map(adminPerformanceRow).join("")}
              </tbody>
            </table>
          </div>
          <div class="pagination">
            <button class="btn" data-admin-page="${filters.current - 1}" ${filters.current <= 1 ? "disabled" : ""}>上一页</button>
            <span class="muted">第 ${filters.current} / ${maxPage} 页</span>
            <button class="btn" data-admin-page="${filters.current + 1}" ${filters.current >= maxPage ? "disabled" : ""}>下一页</button>
          </div>
        ` : emptyState("暂无项目", "新增项目后会出现在后台列表中。")}
      </section>
    `;
  }

  function adminPerformanceRow(item) {
    const performanceId = normalizeId(item.performanceId);
    return `
      <tr>
        <td>
          <strong>${escapeHtml(item.title || "-")}</strong>
          <span class="muted">#${escapeHtml(performanceId)}</span>
        </td>
        <td>${escapeHtml(typeText(item.performanceType))}</td>
        <td>${escapeHtml(item.artist?.name || "-")}</td>
        <td>${escapeHtml(item.venue?.name || "-")}</td>
        <td>${adminStatusPill(item.status)}</td>
        <td>${saleStatusPill(item.saleStatus)}</td>
        <td>${formatDateTime(item.earliestShowTime)}</td>
        <td>
          <div class="button-row">
            <button class="btn compact" type="button" data-action="open-admin-performance-detail" data-performance-id="${escapeAttr(performanceId)}">详情</button>
            <button class="btn compact" type="button" data-action="open-admin-performance-form" data-performance-id="${escapeAttr(performanceId)}">编辑</button>
            <button class="btn danger compact" type="button" data-action="delete-admin-performance" data-performance-id="${escapeAttr(performanceId)}" ${isOnSale(item) ? "disabled" : ""}>删除</button>
          </div>
        </td>
      </tr>
    `;
  }

  function bindAdminPerformanceList(filters) {
    const form = document.querySelector("#admin-performance-filter");
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      const data = Object.fromEntries(new FormData(form).entries());
      const params = new URLSearchParams();
      for (const key of ["title", "performanceType", "status"]) {
        if (data[key]) params.set(key, data[key]);
      }
      window.location.hash = `#/admin/performances${params.toString() ? `?${params.toString()}` : ""}`;
    });
    document.querySelectorAll("[data-admin-page]").forEach((button) => {
      button.addEventListener("click", () => {
        const targetPage = Number(button.dataset.adminPage);
        if (!targetPage || targetPage < 1) return;
        const params = new URLSearchParams();
        for (const key of ["title", "performanceType", "status"]) {
          if (filters[key]) params.set(key, filters[key]);
        }
        params.set("current", targetPage);
        window.location.hash = `#/admin/performances?${params.toString()}`;
      });
    });
  }

  async function openAdminPerformanceDetail(performanceId) {
    if (!requireLogin()) return;
    const resolvedPerformanceId = normalizeId(performanceId);
    if (!/^\d+$/.test(resolvedPerformanceId)) {
      toast("项目编号无效，无法查看详情", "error");
      return;
    }
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog wide admin-performance-detail-dialog" role="dialog" aria-modal="true" aria-labelledby="admin-performance-detail-title">
          <div class="dialog-head">
            <h2 id="admin-performance-detail-title">项目详情</h2>
            <button class="btn compact" type="button" data-action="close-dialog">关闭</button>
          </div>
          <div class="dialog-body admin-performance-detail-body">${inlineEmpty("正在加载", "请稍候。")}</div>
        </section>
      </div>
    `;
    try {
      const [detail, sessions] = await Promise.all([
        apiGet(`/api/performance/admin/${encodeURIComponent(resolvedPerformanceId)}`),
        loadAdminShows(resolvedPerformanceId)
      ]);
      dialogRoot.querySelector(".dialog-body").innerHTML = adminPerformanceDetailTemplate(detail, sessions);
    } catch (error) {
      dialogRoot.querySelector(".dialog-body").innerHTML = `<p class="form-error">${escapeHtml(apiMessage(error))}</p>`;
      toast(apiMessage(error), "error");
    }
  }

  function adminPerformanceDetailTemplate(detail, showSessions) {
    const sessions = Array.isArray(showSessions) ? showSessions : [];
    const performanceId = normalizeId(detail.performanceId);
    const onSale = isOnSale(detail);
    return `
      <div class="admin-detail admin-performance-detail">
        <div class="management-top admin-performance-titlebar">
          <div>
            <h3>${escapeHtml(detail.title || "-")}</h3>
            <p>${escapeHtml(typeText(detail.performanceType))} · ${escapeHtml(detail.artist?.name || "-")}</p>
          </div>
          <div class="status-stack">
            ${adminStatusPill(detail.status)}
            ${saleStatusPill(detail.saleStatus)}
          </div>
        </div>
        <ul class="meta-list admin-performance-meta">
          <li><strong>项目编号</strong> ${escapeHtml(detail.performanceId)}</li>
          <li><strong>场馆</strong> ${escapeHtml(detail.venue?.name || "-")}</li>
          <li><strong>地址</strong> ${escapeHtml(formatVenueAddress(detail.venue) || "-")}</li>
          <li><strong>开售状态</strong> ${escapeHtml(saleStatusText(detail.saleStatus))}</li>
          <li><strong>定时开售</strong> ${formatDateTime(detail.scheduledSaleTime)}</li>
          <li><strong>实际开售</strong> ${formatDateTime(detail.actualSaleTime)}</li>
          <li><strong>海报</strong> ${detail.posterUrl ? `<a href="${escapeAttr(detail.posterUrl)}" target="_blank" rel="noreferrer">打开海报</a>` : "-"}</li>
          <li><strong>简介</strong> ${escapeHtml(detail.description || "-")}</li>
        </ul>
        <section class="state-box admin-sale-panel">
          <div class="management-top">
            <div>
              <h3>开售管理</h3>
              <p class="muted">${escapeHtml(saleStatusDescription(detail))}</p>
            </div>
            <div class="button-row">
              <button class="btn primary compact" type="button" data-action="start-admin-performance-sale" data-performance-id="${escapeAttr(performanceId)}" ${onSale ? "disabled" : ""}>立即开售</button>
              <button class="btn compact" type="button" data-action="open-admin-performance-schedule-sale" data-performance-id="${escapeAttr(performanceId)}" ${onSale ? "disabled" : ""}>定时开售</button>
            </div>
          </div>
        </section>
        <section class="state-box admin-session-panel">
          <div class="management-top">
            <h3>场次管理</h3>
            <button class="btn primary compact" type="button" data-action="open-admin-show-form" data-performance-id="${escapeAttr(performanceId)}" ${onSale ? "disabled" : ""}>新增场次</button>
          </div>
          ${sessions.length ? `
            <div class="management-list admin-session-list">
              ${sessions.map((session) => adminShowSessionCard(performanceId, session, onSale)).join("")}
            </div>
          ` : inlineEmpty("暂无场次", "新增场次后，用户可以在项目详情页选择具体演出时间。")}
        </section>
      </div>
    `;
  }

  function adminShowSessionCard(performanceId, session, performanceOnSale = false) {
    const showId = normalizeId(session.showId);
    return `
      <article class="management-card admin-session-card">
        <div class="management-top">
          <div>
            <h3>${formatDateTime(session.showTime)}</h3>
            <p>时长 ${formatDuration(session.durationMinutes)} · 停售 ${formatDateTime(session.saleEndTime)}</p>
          </div>
          ${adminStatusPill(session.status)}
        </div>
        <div class="button-row">
          <button class="btn compact" type="button" data-action="open-admin-ticket-config" data-performance-id="${escapeAttr(performanceId)}" data-show-id="${escapeAttr(showId)}">票档配置</button>
          <button class="btn compact" type="button" data-action="open-admin-show-form" data-performance-id="${escapeAttr(performanceId)}" data-show-id="${escapeAttr(showId)}" ${performanceOnSale ? "disabled" : ""}>编辑</button>
          <button class="btn danger compact" type="button" data-action="delete-admin-show" data-performance-id="${escapeAttr(performanceId)}" data-show-id="${escapeAttr(showId)}" ${performanceOnSale ? "disabled" : ""}>删除</button>
        </div>
      </article>
    `;
  }

  async function openAdminPerformanceForm(performanceId) {
    if (!requireLogin()) return;
    const resolvedPerformanceId = normalizeId(performanceId);
    const editing = /^\d+$/.test(resolvedPerformanceId);
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog wide" role="dialog" aria-modal="true" aria-labelledby="admin-performance-form-title">
          <div class="dialog-head">
            <h2 id="admin-performance-form-title">${editing ? "编辑项目" : "新增项目"}</h2>
            <button class="btn compact" type="button" data-action="close-dialog">关闭</button>
          </div>
          <div class="dialog-body">${inlineEmpty("正在加载", "请稍候。")}</div>
        </section>
      </div>
    `;
    try {
      const [artists, venues, detail] = await Promise.all([
        loadAdminArtists(),
        loadAdminVenues(),
        editing ? apiGet(`/api/performance/admin/${encodeURIComponent(resolvedPerformanceId)}`) : Promise.resolve(null)
      ]);
      dialogRoot.querySelector(".dialog-body").innerHTML = adminPerformanceFormTemplate(detail, artists, venues);
      bindAdminPerformanceForm(editing, resolvedPerformanceId);
      dialogRoot.querySelector("input[name='title']")?.focus();
    } catch (error) {
      dialogRoot.querySelector(".dialog-body").innerHTML = `<p class="form-error">${escapeHtml(apiMessage(error))}</p>`;
      toast(apiMessage(error), "error");
    }
  }

  function adminPerformanceFormTemplate(detail, artists, venues) {
    const artistId = normalizeId(detail?.artist?.artistId);
    const venueId = normalizeId(detail?.venue?.venueId);
    const status = String(detail?.status ?? 1);
    return `
      <form id="admin-performance-form" class="form-grid two-col">
        <div class="field two-col-span">
          <label for="admin-title">项目名称</label>
          <input id="admin-title" name="title" value="${escapeAttr(detail?.title || "")}" maxlength="255" required>
        </div>
        <div class="field">
          <label for="admin-type">类型</label>
          <select id="admin-type" name="performanceType" required>
            ${performanceTypeOptions(detail?.performanceType || "CONCERT", false)}
          </select>
        </div>
        <div class="field">
          <label for="admin-status">状态</label>
          <select id="admin-status" name="status" required>
            ${option("1", "启用", status)}
            ${option("0", "停用", status)}
          </select>
        </div>
        <div class="field">
          <label for="admin-artist">艺人/团队</label>
          <select id="admin-artist" name="artistId" required>
            ${artists.map((artist) => option(normalizeId(artist.artistId), artist.name || `艺人 ${artist.artistId}`, artistId)).join("")}
          </select>
        </div>
        <div class="field">
          <label for="admin-venue">场馆</label>
          <select id="admin-venue" name="venueId" required>
            ${venues.map((venue) => option(normalizeId(venue.venueId), adminVenueLabel(venue), venueId)).join("")}
          </select>
        </div>
        <div class="field two-col-span">
          <label for="admin-poster">海报 URL</label>
          <input id="admin-poster" name="posterUrl" value="${escapeAttr(detail?.posterUrl || "")}" maxlength="512" placeholder="https://...">
        </div>
        <div class="field two-col-span">
          <label for="admin-description">简介</label>
          <textarea id="admin-description" name="description" rows="5" maxlength="1024">${escapeHtml(detail?.description || "")}</textarea>
        </div>
        <p class="form-error two-col-span" data-admin-performance-error hidden></p>
        <div class="button-row two-col-span">
          <button class="btn primary" type="submit" data-admin-performance-submit>${detail ? "保存项目" : "创建项目"}</button>
          <button class="btn" type="button" data-action="close-dialog">取消</button>
        </div>
      </form>
    `;
  }

  function bindAdminPerformanceForm(editing, performanceId) {
    const form = dialogRoot.querySelector("#admin-performance-form");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const payload = validateAdminPerformanceForm(form);
      const errorNode = form.querySelector("[data-admin-performance-error]");
      if (!payload.valid) {
        showFormError(errorNode, payload.message);
        return;
      }
      const submit = form.querySelector("[data-admin-performance-submit]");
      submit.disabled = true;
      submit.textContent = "保存中";
      showFormError(errorNode, "");
      try {
        if (editing) {
          await apiPut(`/api/performance/admin/${encodeURIComponent(performanceId)}`, payload.data);
          toast("项目已更新", "success");
        } else {
          await apiPost("/api/performance/admin", payload.data);
          toast("项目已创建", "success");
        }
        closeDialog();
        renderAdminPerformances(parseHash().query);
      } catch (error) {
        showFormError(errorNode, apiMessage(error));
        toast(apiMessage(error), "error");
        submit.disabled = false;
        submit.textContent = editing ? "保存项目" : "创建项目";
      }
    });
  }

  function validateAdminPerformanceForm(form) {
    const data = Object.fromEntries(new FormData(form).entries());
    const title = String(data.title || "").trim();
    const performanceType = String(data.performanceType || "").trim();
    const artistId = normalizeId(data.artistId);
    const venueId = normalizeId(data.venueId);
    const posterUrl = String(data.posterUrl || "").trim();
    const description = String(data.description || "").trim();
    const status = Number(data.status);
    if (!title) return { valid: false, message: "请输入项目名称" };
    if (title.length > 255) return { valid: false, message: "项目名称不能超过 255 个字符" };
    if (!performanceType) return { valid: false, message: "请选择项目类型" };
    if (!/^\d+$/.test(artistId)) return { valid: false, message: "请选择艺人/团队" };
    if (!/^\d+$/.test(venueId)) return { valid: false, message: "请选择场馆" };
    if (posterUrl.length > 512) return { valid: false, message: "海报 URL 不能超过 512 个字符" };
    if (description.length > 1024) return { valid: false, message: "简介不能超过 1024 个字符" };
    if (status !== 0 && status !== 1) return { valid: false, message: "请选择项目状态" };
    return {
      valid: true,
      data: {
        title,
        performanceType,
        artistId,
        venueId,
        posterUrl,
        description,
        status
      }
    };
  }

  async function deleteAdminPerformance(performanceId) {
    if (!requireLogin()) return;
    const resolvedPerformanceId = normalizeId(performanceId);
    if (!/^\d+$/.test(resolvedPerformanceId)) {
      toast("项目编号无效，无法删除", "error");
      return;
    }
    if (!window.confirm("确认删除该演出项目？删除后前台列表不再展示。")) {
      return;
    }
    try {
      await apiDelete(`/api/performance/admin/${encodeURIComponent(resolvedPerformanceId)}`);
      toast("项目已删除", "success");
      renderAdminPerformances(parseHash().query);
    } catch (error) {
      toast(apiMessage(error), "error");
    }
  }

  async function startAdminPerformanceSale(performanceId) {
    if (!requireLogin()) return;
    const resolvedPerformanceId = normalizeId(performanceId);
    if (!/^\d+$/.test(resolvedPerformanceId)) {
      toast("项目编号无效，无法开售", "error");
      return;
    }
    if (!window.confirm("确认立即开售该演出项目？开售后将不能再编辑场次和票档配置。")) {
      return;
    }
    try {
      await apiPost(`/api/performance/admin/${encodeURIComponent(resolvedPerformanceId)}/sale/start-now`, {});
      toast("项目已开售", "success");
      await openAdminPerformanceDetail(resolvedPerformanceId);
      renderAdminPerformances(parseHash().query);
    } catch (error) {
      toast(apiMessage(error), "error");
    }
  }

  function openAdminPerformanceScheduleSale(performanceId) {
    if (!requireLogin()) return;
    const resolvedPerformanceId = normalizeId(performanceId);
    if (!/^\d+$/.test(resolvedPerformanceId)) {
      toast("项目编号无效，无法设置定时开售", "error");
      return;
    }
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="admin-sale-schedule-title">
          <div class="dialog-head">
            <h2 id="admin-sale-schedule-title">定时开售</h2>
            <button class="btn compact" type="button" data-action="open-admin-performance-detail" data-performance-id="${escapeAttr(resolvedPerformanceId)}">返回详情</button>
          </div>
          <div class="dialog-body">
            <form id="admin-sale-schedule-form" class="form-grid">
              <div class="field">
                <label for="admin-sale-start-time">开售时间</label>
                <input id="admin-sale-start-time" name="saleStartTime" type="datetime-local" min="${escapeAttr(toDateTimeLocalValue(new Date(Date.now() + 60_000)))}" required>
              </div>
              <p class="form-error" data-admin-sale-schedule-error hidden></p>
              <div class="button-row">
                <button class="btn primary" type="submit" data-admin-sale-schedule-submit>保存定时开售</button>
                <button class="btn" type="button" data-action="open-admin-performance-detail" data-performance-id="${escapeAttr(resolvedPerformanceId)}">取消</button>
              </div>
            </form>
          </div>
        </section>
      </div>
    `;
    bindAdminPerformanceScheduleSaleForm(resolvedPerformanceId);
    dialogRoot.querySelector("input[name='saleStartTime']")?.focus();
  }

  function bindAdminPerformanceScheduleSaleForm(performanceId) {
    const form = dialogRoot.querySelector("#admin-sale-schedule-form");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const saleStartTime = String(new FormData(form).get("saleStartTime") || "").trim();
      const errorNode = form.querySelector("[data-admin-sale-schedule-error]");
      if (!saleStartTime) {
        showFormError(errorNode, "请选择开售时间");
        return;
      }
      const normalized = normalizeDateTimeInput(saleStartTime);
      const selectedTime = new Date(normalized);
      if (!Number.isFinite(selectedTime.getTime()) || selectedTime.getTime() <= Date.now()) {
        showFormError(errorNode, "开售时间必须晚于当前时间");
        return;
      }
      const submit = form.querySelector("[data-admin-sale-schedule-submit]");
      submit.disabled = true;
      submit.textContent = "保存中";
      showFormError(errorNode, "");
      try {
        await apiPost(`/api/performance/admin/${encodeURIComponent(performanceId)}/sale/schedule`, { saleStartTime: normalized });
        toast("定时开售已保存", "success");
        await openAdminPerformanceDetail(performanceId);
        renderAdminPerformances(parseHash().query);
      } catch (error) {
        showFormError(errorNode, apiMessage(error));
        toast(apiMessage(error), "error");
        submit.disabled = false;
        submit.textContent = "保存定时开售";
      }
    });
  }

  async function renderAdminArtists(query) {
    if (!requireLogin()) return;
    const filters = {
      keyword: query.keyword || ""
    };
    renderLoading("正在加载艺人/团队");
    try {
      const artists = await apiGet("/api/performance/admin/artists");
      state.adminOptions.artists = Array.isArray(artists) ? artists : [];
      const records = state.adminOptions.artists.filter((artist) => matchesKeyword([
        artist.name,
        artist.description,
        normalizeId(artist.artistId)
      ], filters.keyword));
      app.innerHTML = adminArtistTemplate(records, filters);
      bindAdminArtistList();
    } catch (error) {
      renderError("艺人/团队加载失败", error, () => renderAdminArtists(query));
    }
  }

  function adminArtistTemplate(records, filters) {
    return `
      <section class="band">
        <div class="section-title">
          <div>
            <h1>艺人/团队管理</h1>
            <p>共 ${records.length} 个艺人或团队</p>
          </div>
          <button class="btn primary" type="button" data-action="open-admin-artist-form">新增艺人/团队</button>
        </div>
        ${adminTabs("artists")}
        <form id="admin-artist-filter" class="toolbar compact-toolbar">
          <div class="field">
            <label for="admin-artist-keyword">关键词</label>
            <input id="admin-artist-keyword" name="keyword" value="${escapeAttr(filters.keyword)}" placeholder="名称、简介或编号">
          </div>
          <div class="button-row">
            <button class="btn primary" type="submit">查询</button>
            <a class="btn" href="#/admin/artists">重置</a>
          </div>
        </form>
        ${records.length ? `
          <div class="admin-table-wrap panel">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>艺人/团队</th>
                  <th>简介</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                ${records.map(adminArtistRow).join("")}
              </tbody>
            </table>
          </div>
        ` : emptyState("暂无艺人/团队", "新增艺人或团队后，可在演出项目表单中选择。")}
      </section>
    `;
  }

  function adminArtistRow(artist) {
    const artistId = normalizeId(artist.artistId);
    return `
      <tr>
        <td>
          <strong>${escapeHtml(artist.name || "-")}</strong>
          <span class="muted">#${escapeHtml(artistId)}</span>
        </td>
        <td>${escapeHtml(artist.description || "-")}</td>
        <td>
          <div class="button-row">
            <button class="btn compact" type="button" data-action="open-admin-artist-form" data-artist-id="${escapeAttr(artistId)}">编辑</button>
            <button class="btn danger compact" type="button" data-action="delete-admin-artist" data-artist-id="${escapeAttr(artistId)}">删除</button>
          </div>
        </td>
      </tr>
    `;
  }

  function bindAdminArtistList() {
    const form = document.querySelector("#admin-artist-filter");
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      const keyword = textField(form, "keyword");
      window.location.hash = `#/admin/artists${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ""}`;
    });
  }

  async function openAdminArtistForm(artistId) {
    if (!requireLogin()) return;
    const resolvedArtistId = normalizeId(artistId);
    const editing = /^\d+$/.test(resolvedArtistId);
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="admin-artist-form-title">
          <div class="dialog-head">
            <h2 id="admin-artist-form-title">${editing ? "编辑艺人/团队" : "新增艺人/团队"}</h2>
            <button class="btn compact" type="button" data-action="close-dialog">关闭</button>
          </div>
          <div class="dialog-body">${inlineEmpty("正在加载", "请稍候。")}</div>
        </section>
      </div>
    `;
    try {
      const detail = editing ? await apiGet(`/api/performance/admin/artists/${encodeURIComponent(resolvedArtistId)}`) : null;
      dialogRoot.querySelector(".dialog-body").innerHTML = adminArtistFormTemplate(detail);
      bindAdminArtistForm(editing, resolvedArtistId);
      dialogRoot.querySelector("input[name='name']")?.focus();
    } catch (error) {
      dialogRoot.querySelector(".dialog-body").innerHTML = `<p class="form-error">${escapeHtml(apiMessage(error))}</p>`;
      toast(apiMessage(error), "error");
    }
  }

  function adminArtistFormTemplate(detail) {
    return `
      <form id="admin-artist-form" class="form-grid">
        <div class="field">
          <label for="admin-artist-name">名称</label>
          <input id="admin-artist-name" name="name" value="${escapeAttr(detail?.name || "")}" maxlength="128" required>
        </div>
        <div class="field">
          <label for="admin-artist-description">简介</label>
          <textarea id="admin-artist-description" name="description" rows="5" maxlength="512">${escapeHtml(detail?.description || "")}</textarea>
        </div>
        <p class="form-error" data-admin-artist-error hidden></p>
        <div class="button-row">
          <button class="btn primary" type="submit" data-admin-artist-submit>${detail ? "保存艺人/团队" : "创建艺人/团队"}</button>
          <button class="btn" type="button" data-action="close-dialog">取消</button>
        </div>
      </form>
    `;
  }

  function bindAdminArtistForm(editing, artistId) {
    const form = dialogRoot.querySelector("#admin-artist-form");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const payload = validateAdminArtistForm(form);
      const errorNode = form.querySelector("[data-admin-artist-error]");
      if (!payload.valid) {
        showFormError(errorNode, payload.message);
        return;
      }
      const submit = form.querySelector("[data-admin-artist-submit]");
      submit.disabled = true;
      submit.textContent = "保存中";
      showFormError(errorNode, "");
      try {
        if (editing) {
          await apiPut(`/api/performance/admin/artists/${encodeURIComponent(artistId)}`, payload.data);
          toast("艺人/团队已更新", "success");
        } else {
          await apiPost("/api/performance/admin/artists", payload.data);
          toast("艺人/团队已创建", "success");
        }
        state.adminOptions.artists = null;
        closeDialog();
        renderAdminArtists(parseHash().query);
      } catch (error) {
        showFormError(errorNode, apiMessage(error));
        toast(apiMessage(error), "error");
        submit.disabled = false;
        submit.textContent = editing ? "保存艺人/团队" : "创建艺人/团队";
      }
    });
  }

  function validateAdminArtistForm(form) {
    const name = textField(form, "name");
    const description = textField(form, "description");
    if (!name) return { valid: false, message: "请输入艺人/团队名称" };
    if (name.length > 128) return { valid: false, message: "艺人/团队名称不能超过 128 个字符" };
    if (description.length > 512) return { valid: false, message: "简介不能超过 512 个字符" };
    return {
      valid: true,
      data: {
        name,
        description
      }
    };
  }

  async function deleteAdminArtist(artistId) {
    if (!requireLogin()) return;
    const resolvedArtistId = normalizeId(artistId);
    if (!/^\d+$/.test(resolvedArtistId)) {
      toast("艺人/团队编号无效，无法删除", "error");
      return;
    }
    if (!window.confirm("确认删除该艺人/团队？已被项目引用时后端会拒绝删除。")) {
      return;
    }
    try {
      await apiDelete(`/api/performance/admin/artists/${encodeURIComponent(resolvedArtistId)}`);
      state.adminOptions.artists = null;
      toast("艺人/团队已删除", "success");
      renderAdminArtists(parseHash().query);
    } catch (error) {
      toast(apiMessage(error), "error");
    }
  }

  async function renderAdminVenues(query) {
    if (!requireLogin()) return;
    const filters = {
      keyword: query.keyword || ""
    };
    renderLoading("正在加载场馆");
    try {
      const venues = await apiGet("/api/performance/admin/venues");
      state.adminOptions.venues = Array.isArray(venues) ? venues : [];
      const records = state.adminOptions.venues.filter((venue) => matchesKeyword([
        venue.name,
        venue.city,
        venue.district,
        formatVenueAddress(venue),
        normalizeId(venue.venueId)
      ], filters.keyword));
      app.innerHTML = adminVenueTemplate(records, filters);
      bindAdminVenueList();
    } catch (error) {
      renderError("场馆加载失败", error, () => renderAdminVenues(query));
    }
  }

  function adminVenueTemplate(records, filters) {
    return `
      <section class="band">
        <div class="section-title">
          <div>
            <h1>场馆管理</h1>
            <p>共 ${records.length} 个场馆</p>
          </div>
          <button class="btn primary" type="button" data-action="open-admin-venue-form">新增场馆</button>
        </div>
        ${adminTabs("venues")}
        <form id="admin-venue-filter" class="toolbar compact-toolbar">
          <div class="field">
            <label for="admin-venue-keyword">关键词</label>
            <input id="admin-venue-keyword" name="keyword" value="${escapeAttr(filters.keyword)}" placeholder="名称、城市、地址或编号">
          </div>
          <div class="button-row">
            <button class="btn primary" type="submit">查询</button>
            <a class="btn" href="#/admin/venues">重置</a>
          </div>
        </form>
        ${records.length ? `
          <div class="admin-table-wrap panel">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>场馆</th>
                  <th>区域</th>
                  <th>地址</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                ${records.map(adminVenueRow).join("")}
              </tbody>
            </table>
          </div>
        ` : emptyState("暂无场馆", "新增场馆后，可在演出项目表单中选择。")}
      </section>
    `;
  }

  function adminVenueRow(venue) {
    const venueId = normalizeId(venue.venueId);
    return `
      <tr>
        <td>
          <strong>${escapeHtml(venue.name || "-")}</strong>
          <span class="muted">#${escapeHtml(venueId)}</span>
        </td>
        <td>${escapeHtml([venue.province, venue.city, venue.district].filter(Boolean).join(" / ") || "-")}</td>
        <td>${escapeHtml(formatVenueAddress(venue) || "-")}</td>
        <td>
          <div class="button-row">
            <button class="btn compact" type="button" data-action="open-admin-venue-seats" data-venue-id="${escapeAttr(venueId)}">座位</button>
            <button class="btn compact" type="button" data-action="open-admin-venue-form" data-venue-id="${escapeAttr(venueId)}">编辑</button>
            <button class="btn danger compact" type="button" data-action="delete-admin-venue" data-venue-id="${escapeAttr(venueId)}">删除</button>
          </div>
        </td>
      </tr>
    `;
  }

  function bindAdminVenueList() {
    const form = document.querySelector("#admin-venue-filter");
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      const keyword = textField(form, "keyword");
      window.location.hash = `#/admin/venues${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ""}`;
    });
  }

  async function openAdminVenueForm(venueId) {
    if (!requireLogin()) return;
    const resolvedVenueId = normalizeId(venueId);
    const editing = /^\d+$/.test(resolvedVenueId);
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog wide" role="dialog" aria-modal="true" aria-labelledby="admin-venue-form-title">
          <div class="dialog-head">
            <h2 id="admin-venue-form-title">${editing ? "编辑场馆" : "新增场馆"}</h2>
            <button class="btn compact" type="button" data-action="close-dialog">关闭</button>
          </div>
          <div class="dialog-body">${inlineEmpty("正在加载", "请稍候。")}</div>
        </section>
      </div>
    `;
    try {
      const detail = editing ? await apiGet(`/api/performance/admin/venues/${encodeURIComponent(resolvedVenueId)}`) : null;
      dialogRoot.querySelector(".dialog-body").innerHTML = adminVenueFormTemplate(detail);
      bindAdminVenueForm(editing, resolvedVenueId);
      dialogRoot.querySelector("input[name='name']")?.focus();
    } catch (error) {
      dialogRoot.querySelector(".dialog-body").innerHTML = `<p class="form-error">${escapeHtml(apiMessage(error))}</p>`;
      toast(apiMessage(error), "error");
    }
  }

  function adminVenueFormTemplate(detail) {
    return `
      <form id="admin-venue-form" class="form-grid two-col">
        <div class="field two-col-span">
          <label for="admin-venue-name">场馆名称</label>
          <input id="admin-venue-name" name="name" value="${escapeAttr(detail?.name || "")}" maxlength="128" required>
        </div>
        <div class="field">
          <label for="admin-venue-country">国家/地区</label>
          <input id="admin-venue-country" name="country" value="${escapeAttr(detail?.country || "中国")}" maxlength="64">
        </div>
        <div class="field">
          <label for="admin-venue-province">省/直辖市</label>
          <input id="admin-venue-province" name="province" value="${escapeAttr(detail?.province || "")}" maxlength="64" required>
        </div>
        <div class="field">
          <label for="admin-venue-city">城市</label>
          <input id="admin-venue-city" name="city" value="${escapeAttr(detail?.city || "")}" maxlength="64" required>
        </div>
        <div class="field">
          <label for="admin-venue-district">区/县</label>
          <input id="admin-venue-district" name="district" value="${escapeAttr(detail?.district || "")}" maxlength="64" required>
        </div>
        <div class="field">
          <label for="admin-venue-street">街道</label>
          <input id="admin-venue-street" name="street" value="${escapeAttr(detail?.street || "")}" maxlength="128" required>
        </div>
        <div class="field">
          <label for="admin-venue-house-number">门牌号</label>
          <input id="admin-venue-house-number" name="houseNumber" value="${escapeAttr(detail?.houseNumber || "")}" maxlength="64" required>
        </div>
        <div class="field">
          <label for="admin-venue-town">乡镇/街道办</label>
          <input id="admin-venue-town" name="town" value="${escapeAttr(detail?.town || "")}" maxlength="64">
        </div>
        <div class="field">
          <label for="admin-venue-village">村/社区</label>
          <input id="admin-venue-village" name="village" value="${escapeAttr(detail?.village || "")}" maxlength="64">
        </div>
        <div class="field">
          <label for="admin-venue-estate">园区/小区</label>
          <input id="admin-venue-estate" name="estate" value="${escapeAttr(detail?.estate || "")}" maxlength="128">
        </div>
        <div class="field">
          <label for="admin-venue-building">楼栋</label>
          <input id="admin-venue-building" name="building" value="${escapeAttr(detail?.building || "")}" maxlength="128">
        </div>
        <div class="field two-col-span">
          <label for="admin-venue-address">完整地址</label>
          <input id="admin-venue-address" name="address" value="${escapeAttr(detail?.address || "")}" maxlength="255" placeholder="留空时自动按上方地址字段拼接">
        </div>
        <div class="field">
          <label for="admin-venue-seat-rows">座位行数</label>
          <input id="admin-venue-seat-rows" name="seatRowCount" type="number" min="1" max="200" step="1" value="${escapeAttr(detail?.seatRowCount || 10)}" required>
        </div>
        <div class="field">
          <label for="admin-venue-seat-columns">座位列数</label>
          <input id="admin-venue-seat-columns" name="seatColumnCount" type="number" min="1" max="200" step="1" value="${escapeAttr(detail?.seatColumnCount || 10)}" required>
        </div>
        <p class="form-error two-col-span" data-admin-venue-error hidden></p>
        <div class="button-row two-col-span">
          <button class="btn primary" type="submit" data-admin-venue-submit>${detail ? "保存场馆" : "创建场馆"}</button>
          <button class="btn" type="button" data-action="close-dialog">取消</button>
        </div>
      </form>
    `;
  }

  function bindAdminVenueForm(editing, venueId) {
    const form = dialogRoot.querySelector("#admin-venue-form");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const payload = validateAdminVenueForm(form);
      const errorNode = form.querySelector("[data-admin-venue-error]");
      if (!payload.valid) {
        showFormError(errorNode, payload.message);
        return;
      }
      const submit = form.querySelector("[data-admin-venue-submit]");
      submit.disabled = true;
      submit.textContent = "保存中";
      showFormError(errorNode, "");
      try {
        if (editing) {
          await apiPut(`/api/performance/admin/venues/${encodeURIComponent(venueId)}`, payload.data);
          toast("场馆已更新", "success");
        } else {
          await apiPost("/api/performance/admin/venues", payload.data);
          toast("场馆已创建", "success");
        }
        state.adminOptions.venues = null;
        closeDialog();
        renderAdminVenues(parseHash().query);
      } catch (error) {
        showFormError(errorNode, apiMessage(error));
        toast(apiMessage(error), "error");
        submit.disabled = false;
        submit.textContent = editing ? "保存场馆" : "创建场馆";
      }
    });
  }

  function validateAdminVenueForm(form) {
    const data = Object.fromEntries(new FormData(form).entries());
    const payload = {
      name: String(data.name || "").trim(),
      country: String(data.country || "").trim(),
      province: String(data.province || "").trim(),
      city: String(data.city || "").trim(),
      district: String(data.district || "").trim(),
      town: String(data.town || "").trim(),
      village: String(data.village || "").trim(),
      street: String(data.street || "").trim(),
      houseNumber: String(data.houseNumber || "").trim(),
      estate: String(data.estate || "").trim(),
      building: String(data.building || "").trim(),
      address: String(data.address || "").trim(),
      seatRowCount: Number(data.seatRowCount),
      seatColumnCount: Number(data.seatColumnCount)
    };
    const required = [
      ["name", "请输入场馆名称"],
      ["province", "请输入省/直辖市"],
      ["city", "请输入城市"],
      ["district", "请输入区/县"],
      ["street", "请输入街道"],
      ["houseNumber", "请输入门牌号"]
    ];
    for (const [key, message] of required) {
      if (!payload[key]) return { valid: false, message };
    }
    const maxLengths = {
      name: 128,
      country: 64,
      province: 64,
      city: 64,
      district: 64,
      town: 64,
      village: 64,
      street: 128,
      houseNumber: 64,
      estate: 128,
      building: 128,
      address: 255
    };
    for (const [key, max] of Object.entries(maxLengths)) {
      if (payload[key].length > max) {
        return { valid: false, message: "场馆字段长度超出限制" };
      }
    }
    if (!Number.isInteger(payload.seatRowCount) || payload.seatRowCount < 1 || payload.seatRowCount > 200) {
      return { valid: false, message: "座位行数需要在 1 到 200 之间" };
    }
    if (!Number.isInteger(payload.seatColumnCount) || payload.seatColumnCount < 1 || payload.seatColumnCount > 200) {
      return { valid: false, message: "座位列数需要在 1 到 200 之间" };
    }
    if (payload.seatRowCount * payload.seatColumnCount > 10000) {
      return { valid: false, message: "座位总数不能超过 10000" };
    }
    return { valid: true, data: payload };
  }

  async function deleteAdminVenue(venueId) {
    if (!requireLogin()) return;
    const resolvedVenueId = normalizeId(venueId);
    if (!/^\d+$/.test(resolvedVenueId)) {
      toast("场馆编号无效，无法删除", "error");
      return;
    }
    if (!window.confirm("确认删除该场馆？已被项目引用时后端会拒绝删除。")) {
      return;
    }
    try {
      await apiDelete(`/api/performance/admin/venues/${encodeURIComponent(resolvedVenueId)}`);
      state.adminOptions.venues = null;
      toast("场馆已删除", "success");
      renderAdminVenues(parseHash().query);
    } catch (error) {
      toast(apiMessage(error), "error");
    }
  }

  async function openAdminVenueSeats(venueId) {
    if (!requireLogin()) return;
    const resolvedVenueId = normalizeId(venueId);
    if (!/^\d+$/.test(resolvedVenueId)) {
      toast("场馆编号无效，无法管理座位", "error");
      return;
    }
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog xwide" role="dialog" aria-modal="true" aria-labelledby="admin-seat-title">
          <div class="dialog-head">
            <h2 id="admin-seat-title">座位管理</h2>
            <button class="btn compact" type="button" data-action="close-dialog">关闭</button>
          </div>
          <div class="dialog-body">${inlineEmpty("正在加载", "请稍候。")}</div>
        </section>
      </div>
    `;
    try {
      const [venue, seats] = await Promise.all([
        apiGet(`/api/performance/admin/venues/${encodeURIComponent(resolvedVenueId)}`),
        apiGet(`/api/performance/admin/venues/${encodeURIComponent(resolvedVenueId)}/seats`)
      ]);
      state.adminSeatManager = {
        venue,
        seats: Array.isArray(seats) ? seats : [],
        filter: {
          keyword: "",
          areaName: "",
          status: ""
        }
      };
      renderAdminVenueSeatsDialog();
    } catch (error) {
      dialogRoot.querySelector(".dialog-body").innerHTML = `<p class="form-error">${escapeHtml(apiMessage(error))}</p>`;
      toast(apiMessage(error), "error");
    }
  }

  function renderAdminVenueSeatsDialog() {
    const manager = state.adminSeatManager;
    if (!manager) {
      return;
    }
    dialogRoot.querySelector(".dialog-body").innerHTML = adminVenueSeatsTemplate(manager);
    bindAdminVenueSeatsDialog();
  }

  function adminVenueSeatsTemplate(manager) {
    const venueId = normalizeId(manager.venue?.venueId);
    const seats = Array.isArray(manager.seats) ? manager.seats : [];
    const records = filterAdminVenueSeats(seats, manager.filter);
    const areas = [...new Set(seats.map((seat) => seat.areaName).filter(Boolean))].sort((left, right) => left.localeCompare(right, "zh-CN"));
    return `
      <div class="admin-detail">
        <div class="management-top">
          <div>
            <h3>${escapeHtml(manager.venue?.name || "-")}</h3>
            <p class="muted">共 ${seats.length} 个座位 · ${seatAreaSummary(seats)}</p>
          </div>
          <button class="btn primary compact" type="button" data-action="open-admin-venue-seat-form" data-venue-id="${escapeAttr(venueId)}">新增座位</button>
        </div>
        <form id="admin-seat-filter" class="toolbar">
          <div class="field">
            <label for="admin-seat-keyword">关键字</label>
            <input id="admin-seat-keyword" name="keyword" value="${escapeAttr(manager.filter.keyword)}" placeholder="座位号、行列或编号">
          </div>
          <div class="field">
            <label for="admin-seat-area">座位区域</label>
            <select id="admin-seat-area" name="areaName">
              ${option("", "全部区域", manager.filter.areaName)}
              ${areas.map((area) => option(area, area, manager.filter.areaName)).join("")}
            </select>
          </div>
          <div class="field">
            <label for="admin-seat-status">状态</label>
            <select id="admin-seat-status" name="status">
              ${option("", "全部", manager.filter.status)}
              ${option("1", "启用", manager.filter.status)}
              ${option("0", "停用", manager.filter.status)}
            </select>
          </div>
          <div class="button-row">
            <button class="btn primary" type="submit">筛选</button>
            <button class="btn" type="button" data-admin-seat-filter-reset>重置</button>
          </div>
        </form>
        ${seats.length ? `
          ${adminVenueSeatMapTemplate(manager, records)}
        ` : inlineEmpty("暂无座位", "可新增座位后继续配置演出票档。")}
      </div>
    `;
  }

  function adminVenueSeatMapTemplate(manager, records) {
    const venueId = normalizeId(manager.venue?.venueId);
    const seats = Array.isArray(manager.seats) ? manager.seats : [];
    const rowCount = Number(manager.venue?.seatRowCount || maxSeatCoordinate(seats, "rowNo") || 0);
    const columnCount = Number(manager.venue?.seatColumnCount || maxSeatCoordinate(seats, "columnNo") || 0);
    const byPosition = new Map(seats.map((seat) => [`${Number(seat.rowNo)}:${Number(seat.columnNo)}`, seat]));
    const filteredIds = new Set((records || []).map((seat) => normalizeId(seat.seatId)));
    const rows = [];
    for (let row = 1; row <= rowCount; row += 1) {
      const cells = [];
      for (let column = 1; column <= columnCount; column += 1) {
        cells.push(adminVenueSeatButton(venueId, byPosition.get(`${row}:${column}`), filteredIds));
      }
      rows.push(`<div class="seat-row" style="grid-template-columns: repeat(${Math.max(columnCount, 1)}, minmax(32px, 1fr));">${cells.join("")}</div>`);
    }
    return `
      <div class="seat-wrap admin-seat-map-wrap">
        <div class="management-top">
          <div>
            <h3>座位图</h3>
            <p class="muted">${rowCount} 行 × ${columnCount} 列 · 当前显示 ${records.length} 个座位</p>
          </div>
          <span class="pill">${records.length}/${seats.length}</span>
        </div>
        <div class="stage">舞台 / 银幕</div>
        <div class="seat-map admin-seat-map">${rows.join("")}</div>
        <div class="legend">
          <span><i class="swatch"></i>启用</span>
          <span><i class="swatch sold"></i>停用</span>
          <span><i class="swatch other"></i>筛选外</span>
        </div>
      </div>
    `;
  }

  function adminVenueSeatButton(venueId, seat, filteredIds) {
    if (!seat) {
      return `<button type="button" class="seat missing" disabled aria-label="空位"></button>`;
    }
    const seatId = normalizeId(seat.seatId);
    const inFilter = filteredIds.has(seatId);
    const classNames = ["seat"];
    if (Number(seat.status) !== 1) classNames.push("sold");
    if (!inFilter) classNames.push("filtered");
    const label = `${seat.seatNo || seatId} · ${seat.areaName || "-"} · ${seat.rowNo || "-"}行${seat.columnNo || "-"}列`;
    return `
      <button type="button" class="${classNames.join(" ")}" data-action="open-admin-venue-seat-form" data-venue-id="${escapeAttr(venueId)}" data-seat-id="${escapeAttr(seatId)}" title="${escapeAttr(label)}" aria-label="${escapeAttr(label)}">
        ${escapeHtml(shortSeatNo(seat.seatNo))}
      </button>
    `;
  }

  function maxSeatCoordinate(seats, field) {
    return (seats || []).reduce((max, seat) => Math.max(max, Number(seat?.[field] || 0)), 0);
  }

  function filterAdminVenueSeats(seats, filter) {
    const keyword = String(filter?.keyword || "").trim().toLowerCase();
    const areaName = String(filter?.areaName || "").trim();
    const status = String(filter?.status || "").trim();
    return (seats || []).filter((seat) => {
      const matchesArea = !areaName || seat.areaName === areaName;
      const matchesStatus = !status || String(seat.status) === status;
      const matchesText = !keyword || [
        seat.seatId,
        seat.seatNo,
        seat.areaName,
        seat.rowNo,
        seat.columnNo
      ].some((value) => String(value || "").toLowerCase().includes(keyword));
      return matchesArea && matchesStatus && matchesText;
    });
  }

  function seatAreaSummary(seats) {
    const counts = new Map();
    (seats || []).forEach((seat) => {
      const area = seat.areaName || "未分区";
      counts.set(area, (counts.get(area) || 0) + 1);
    });
    if (counts.size === 0) {
      return "0 个区域";
    }
    return [...counts.entries()]
      .sort(([left], [right]) => left.localeCompare(right, "zh-CN"))
      .map(([area, count]) => `${area} ${count}`)
      .join(" · ");
  }

  function bindAdminVenueSeatsDialog() {
    const form = dialogRoot.querySelector("#admin-seat-filter");
    form?.addEventListener("submit", (event) => {
      event.preventDefault();
      state.adminSeatManager.filter = {
        keyword: textField(form, "keyword"),
        areaName: String(new FormData(form).get("areaName") || ""),
        status: String(new FormData(form).get("status") || "")
      };
      renderAdminVenueSeatsDialog();
    });
    dialogRoot.querySelector("[data-admin-seat-filter-reset]")?.addEventListener("click", () => {
      state.adminSeatManager.filter = {
        keyword: "",
        areaName: "",
        status: ""
      };
      renderAdminVenueSeatsDialog();
    });
  }

  async function openAdminVenueSeatForm(venueId, seatId) {
    if (!requireLogin()) return;
    const resolvedVenueId = normalizeId(venueId);
    const resolvedSeatId = normalizeId(seatId);
    if (!/^\d+$/.test(resolvedVenueId)) {
      toast("场馆编号无效，无法维护座位", "error");
      return;
    }
    let manager = state.adminSeatManager;
    if (!manager || normalizeId(manager.venue?.venueId) !== resolvedVenueId) {
      const [venue, seats] = await Promise.all([
        apiGet(`/api/performance/admin/venues/${encodeURIComponent(resolvedVenueId)}`),
        apiGet(`/api/performance/admin/venues/${encodeURIComponent(resolvedVenueId)}/seats`)
      ]);
      manager = {
        venue,
        seats: Array.isArray(seats) ? seats : [],
        filter: {
          keyword: "",
          areaName: "",
          status: ""
        }
      };
      state.adminSeatManager = manager;
    }
    const editing = /^\d+$/.test(resolvedSeatId);
    const seat = editing ? manager.seats.find((item) => normalizeId(item.seatId) === resolvedSeatId) : null;
    if (editing && !seat) {
      toast("座位不存在或已被删除", "error");
      return;
    }
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="admin-seat-form-title">
          <div class="dialog-head">
            <h2 id="admin-seat-form-title">${editing ? "编辑座位" : "新增座位"}</h2>
            <button class="btn compact" type="button" data-action="open-admin-venue-seats" data-venue-id="${escapeAttr(resolvedVenueId)}">返回座位</button>
          </div>
          <div class="dialog-body">
            ${adminVenueSeatFormTemplate(resolvedVenueId, seat)}
          </div>
        </section>
      </div>
    `;
    bindAdminVenueSeatForm(editing, resolvedVenueId, resolvedSeatId);
    dialogRoot.querySelector("input[name='areaName']")?.focus();
  }

  function adminVenueSeatFormTemplate(venueId, seat) {
    const status = String(seat?.status ?? 1);
    return `
      <form id="admin-seat-form" class="form-grid two-col">
        <div class="field two-col-span">
          <label for="admin-seat-area-name">座位区域</label>
          <input id="admin-seat-area-name" name="areaName" value="${escapeAttr(seat?.areaName || "")}" maxlength="64" required>
        </div>
        <div class="field">
          <label for="admin-seat-row">排/行</label>
          <input id="admin-seat-row" name="rowNo" type="number" min="1" max="1000" step="1" value="${escapeAttr(seat?.rowNo || "")}" required>
        </div>
        <div class="field">
          <label for="admin-seat-column">列</label>
          <input id="admin-seat-column" name="columnNo" type="number" min="1" max="1000" step="1" value="${escapeAttr(seat?.columnNo || "")}" required>
        </div>
        <div class="field">
          <label for="admin-seat-no">座位号</label>
          <input id="admin-seat-no" name="seatNo" value="${escapeAttr(seat?.seatNo || "")}" maxlength="32" required>
        </div>
        <div class="field">
          <label for="admin-seat-form-status">状态</label>
          <select id="admin-seat-form-status" name="status" required>
            ${option("1", "启用", status)}
            ${option("0", "停用", status)}
          </select>
        </div>
        <p class="form-error two-col-span" data-admin-seat-error hidden></p>
        <div class="button-row two-col-span">
          <button class="btn primary" type="submit" data-admin-seat-submit>${seat ? "保存座位" : "创建座位"}</button>
          <button class="btn" type="button" data-action="open-admin-venue-seats" data-venue-id="${escapeAttr(venueId)}">取消</button>
        </div>
      </form>
    `;
  }

  function bindAdminVenueSeatForm(editing, venueId, seatId) {
    const form = dialogRoot.querySelector("#admin-seat-form");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const payload = validateAdminSeatForm(form);
      const errorNode = form.querySelector("[data-admin-seat-error]");
      if (!payload.valid) {
        showFormError(errorNode, payload.message);
        return;
      }
      const submit = form.querySelector("[data-admin-seat-submit]");
      submit.disabled = true;
      submit.textContent = "保存中";
      showFormError(errorNode, "");
      try {
        if (editing) {
          await apiPut(`/api/performance/admin/venues/${encodeURIComponent(venueId)}/seats/${encodeURIComponent(seatId)}`, payload.data);
          toast("座位已更新", "success");
        } else {
          await apiPost(`/api/performance/admin/venues/${encodeURIComponent(venueId)}/seats`, payload.data);
          toast("座位已创建", "success");
        }
        await openAdminVenueSeats(venueId);
      } catch (error) {
        showFormError(errorNode, apiMessage(error));
        toast(apiMessage(error), "error");
        submit.disabled = false;
        submit.textContent = editing ? "保存座位" : "创建座位";
      }
    });
  }

  function validateAdminSeatForm(form) {
    const areaName = textField(form, "areaName");
    const rowNo = Number(new FormData(form).get("rowNo"));
    const columnNo = Number(new FormData(form).get("columnNo"));
    const seatNo = textField(form, "seatNo");
    const status = Number(new FormData(form).get("status"));
    if (!areaName) return { valid: false, message: "请输入座位区域" };
    if (areaName.length > 64) return { valid: false, message: "座位区域不能超过 64 个字符" };
    if (!Number.isInteger(rowNo) || rowNo < 1 || rowNo > 1000) return { valid: false, message: "排/行需要在 1 到 1000 之间" };
    if (!Number.isInteger(columnNo) || columnNo < 1 || columnNo > 1000) return { valid: false, message: "列需要在 1 到 1000 之间" };
    if (!seatNo) return { valid: false, message: "请输入座位号" };
    if (seatNo.length > 32) return { valid: false, message: "座位号不能超过 32 个字符" };
    if (status !== 0 && status !== 1) return { valid: false, message: "请选择座位状态" };
    return {
      valid: true,
      data: {
        areaName,
        rowNo,
        columnNo,
        seatNo,
        status
      }
    };
  }

  async function deleteAdminVenueSeat(venueId, seatId) {
    if (!requireLogin()) return;
    const resolvedVenueId = normalizeId(venueId);
    const resolvedSeatId = normalizeId(seatId);
    if (!/^\d+$/.test(resolvedVenueId) || !/^\d+$/.test(resolvedSeatId)) {
      toast("座位编号无效，无法删除", "error");
      return;
    }
    if (!window.confirm("确认删除该座位？已配置到票档的座位后端会拒绝删除。")) {
      return;
    }
    try {
      await apiDelete(`/api/performance/admin/venues/${encodeURIComponent(resolvedVenueId)}/seats/${encodeURIComponent(resolvedSeatId)}`);
      toast("座位已删除", "success");
      await openAdminVenueSeats(resolvedVenueId);
    } catch (error) {
      toast(apiMessage(error), "error");
    }
  }

  async function openAdminShowForm(performanceId, showId) {
    if (!requireLogin()) return;
    const resolvedPerformanceId = normalizeId(performanceId);
    const resolvedShowId = normalizeId(showId);
    const editing = /^\d+$/.test(resolvedShowId);
    if (!/^\d+$/.test(resolvedPerformanceId)) {
      toast("项目编号无效，无法管理场次", "error");
      return;
    }
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog ${editing ? "" : "wide"}" role="dialog" aria-modal="true" aria-labelledby="admin-show-form-title">
          <div class="dialog-head">
            <h2 id="admin-show-form-title">${editing ? "编辑场次" : "新增场次"}</h2>
            <button class="btn compact" type="button" data-action="open-admin-performance-detail" data-performance-id="${escapeAttr(resolvedPerformanceId)}">返回详情</button>
          </div>
          <div class="dialog-body">${inlineEmpty("正在加载", "请稍候。")}</div>
        </section>
      </div>
    `;
    try {
      state.adminShowTicketDraft = null;
      const [sessions, performanceDetail] = await Promise.all([
        editing ? loadAdminShows(resolvedPerformanceId) : Promise.resolve([]),
        editing ? Promise.resolve(null) : apiGet(`/api/performance/admin/${encodeURIComponent(resolvedPerformanceId)}`)
      ]);
      const detail = editing ? sessions.find((item) => normalizeId(item.showId) === resolvedShowId) : null;
      if (editing && !detail) {
        throw new Error("场次不存在或已被删除");
      }
      if (!editing) {
        const venueId = normalizeId(performanceDetail?.venue?.venueId);
        const seatMap = venueId ? await loadAdminVenueSeatMap(venueId, performanceDetail?.venue?.name) : null;
        const seats = Array.isArray(seatMap?.seats) ? seatMap.seats : [];
        state.adminShowTicketDraft = createTicketDraft({
          performanceId: resolvedPerformanceId,
          venueId,
          seatMap: seatMap || venueSeatMapFromSeats(seats, performanceDetail?.venue?.name),
          seats,
          categories: [],
          optional: true,
          readOnly: false
        });
      }
      dialogRoot.querySelector(".dialog-body").innerHTML = adminShowFormTemplate(resolvedPerformanceId, detail);
      bindAdminShowForm(editing, resolvedPerformanceId, resolvedShowId);
      if (!editing) {
        renderAdminShowTicketDraft();
      }
      dialogRoot.querySelector("input[name='showTime']")?.focus();
    } catch (error) {
      dialogRoot.querySelector(".dialog-body").innerHTML = `<p class="form-error">${escapeHtml(apiMessage(error))}</p>`;
      toast(apiMessage(error), "error");
    }
  }

  function adminShowFormTemplate(performanceId, detail) {
    const status = String(detail?.status ?? 1);
    return `
      <form id="admin-show-form" class="form-grid">
        <div class="field">
          <label for="admin-show-time">演出时间</label>
          <input id="admin-show-time" name="showTime" type="datetime-local" value="${escapeAttr(toDateTimeLocalValue(detail?.showTime))}" required>
        </div>
        <div class="field">
          <label for="admin-show-duration">演出时长（分钟）</label>
          <input id="admin-show-duration" name="durationMinutes" type="number" min="1" max="1440" step="1" value="${escapeAttr(detail?.durationMinutes || 120)}" required>
        </div>
        <div class="field">
          <label for="admin-show-status">状态</label>
          <select id="admin-show-status" name="status" required>
            ${option("1", "启用", status)}
            ${option("0", "停用", status)}
          </select>
        </div>
        ${detail ? "" : `<section class="ticket-config-panel" data-admin-show-ticket-editor></section>`}
        <p class="form-error" data-admin-show-error hidden></p>
        <div class="button-row">
          <button class="btn primary" type="submit" data-admin-show-submit>${detail ? "保存场次" : "创建场次"}</button>
          <button class="btn" type="button" data-action="open-admin-performance-detail" data-performance-id="${escapeAttr(performanceId)}">取消</button>
        </div>
      </form>
    `;
  }

  function bindAdminShowForm(editing, performanceId, showId) {
    const form = dialogRoot.querySelector("#admin-show-form");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const payload = validateAdminShowForm(form, { performanceId });
      const errorNode = form.querySelector("[data-admin-show-error]");
      if (!payload.valid) {
        showFormError(errorNode, payload.message);
        return;
      }
      const submit = form.querySelector("[data-admin-show-submit]");
      submit.disabled = true;
      submit.textContent = "保存中";
      showFormError(errorNode, "");
      try {
        if (editing) {
          await apiPut(`/api/performance/admin/${encodeURIComponent(performanceId)}/shows/${encodeURIComponent(showId)}`, payload.data);
          toast("场次已更新", "success");
        } else {
          await apiPost(`/api/performance/admin/${encodeURIComponent(performanceId)}/shows`, payload.data);
          toast("场次已创建", "success");
        }
        state.adminShowTicketDraft = null;
        await openAdminPerformanceDetail(performanceId);
      } catch (error) {
        showFormError(errorNode, apiMessage(error));
        toast(apiMessage(error), "error");
        submit.disabled = false;
        submit.textContent = editing ? "保存场次" : "创建场次";
      }
    });
  }

  function validateAdminShowForm(form, context) {
    const data = Object.fromEntries(new FormData(form).entries());
    const showTime = String(data.showTime || "").trim();
    const durationMinutes = Number(data.durationMinutes);
    const status = Number(data.status);
    const draftPerformanceId = normalizeId(state.adminShowTicketDraft?.performanceId);
    const currentPerformanceId = normalizeId(context?.performanceId);
    if (draftPerformanceId && currentPerformanceId && draftPerformanceId !== currentPerformanceId) {
      return { valid: false, message: "Ticket seat data is stale. Please reopen the show form." };
    }
    const ticketCategories = validateTicketCategoryDraft(state.adminShowTicketDraft, { optional: true });
    if (!showTime) return { valid: false, message: "请选择演出时间" };
    if (!Number.isInteger(durationMinutes) || durationMinutes < 1 || durationMinutes > 1440) {
      return { valid: false, message: "演出时长需要在 1 到 1440 分钟之间" };
    }
    if (status !== 0 && status !== 1) return { valid: false, message: "请选择场次状态" };
    if (!ticketCategories.valid) return { valid: false, message: ticketCategories.message };
    const payload = {
      showTime: normalizeDateTimeInput(showTime),
      durationMinutes,
      status
    };
    if (ticketCategories.data.length) {
      payload.ticketCategories = ticketCategories.data;
    }
    return {
      valid: true,
      data: payload
    };
  }

  async function openAdminTicketConfig(performanceId, showId) {
    if (!requireLogin()) return;
    const resolvedPerformanceId = normalizeId(performanceId);
    const resolvedShowId = normalizeId(showId);
    if (!/^\d+$/.test(resolvedPerformanceId) || !/^\d+$/.test(resolvedShowId)) {
      toast("场次编号无效，无法配置票档", "error");
      return;
    }
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog xwide" role="dialog" aria-modal="true" aria-labelledby="admin-ticket-config-title">
          <div class="dialog-head">
            <h2 id="admin-ticket-config-title">票档配置</h2>
            <button class="btn compact" type="button" data-action="open-admin-performance-detail" data-performance-id="${escapeAttr(resolvedPerformanceId)}">返回详情</button>
          </div>
          <div class="dialog-body">${inlineEmpty("正在加载", "请稍候。")}</div>
        </section>
      </div>
    `;
    try {
      const [configs, seatMap, performanceDetail] = await Promise.all([
        apiGet(`/api/performance/admin/${encodeURIComponent(resolvedPerformanceId)}/shows/${encodeURIComponent(resolvedShowId)}/ticket-categories/config`),
        apiGet(`/api/show/${encodeURIComponent(resolvedShowId)}/seat-map`),
        apiGet(`/api/performance/admin/${encodeURIComponent(resolvedPerformanceId)}`)
      ]);
      const existingConfigs = Array.isArray(configs) ? configs : [];
      state.adminTicketEditor = createTicketDraft({
        performanceId: resolvedPerformanceId,
        showId: resolvedShowId,
        seatMap,
        seats: Array.isArray(seatMap?.seats) ? seatMap.seats : [],
        categories: existingConfigs,
        optional: false,
        readOnly: isOnSale(performanceDetail),
        saleStatus: performanceDetail?.saleStatus
      });
      renderAdminTicketConfigEditor();
    } catch (error) {
      dialogRoot.querySelector(".dialog-body").innerHTML = `<p class="form-error">${escapeHtml(apiMessage(error))}</p>`;
      toast(apiMessage(error), "error");
    }
  }

  function renderAdminTicketConfigEditor() {
    const editor = state.adminTicketEditor;
    if (!editor) {
      return;
    }
    dialogRoot.querySelector(".dialog-body").innerHTML = adminTicketConfigTemplate(editor);
    bindTicketDraftEditor(dialogRoot.querySelector("[data-admin-ticket-editor]"), editor, renderAdminTicketConfigEditor);
    dialogRoot.querySelector("[data-admin-ticket-submit]")?.addEventListener("click", () => submitAdminTicketConfig(editor));
  }

  function adminTicketConfigTemplate(editor) {
    return `
      <div class="admin-detail">
        ${editor.readOnly ? `<p class="form-error">项目已开售，票档和锁座配置仅支持查看。</p>` : ""}
        <section class="ticket-config-panel" data-admin-ticket-editor>
          ${ticketDraftTemplate(editor)}
        </section>
        <div class="button-row">
          ${editor.readOnly ? "" : `<button class="btn primary" type="button" data-admin-ticket-submit>保存票档配置</button>`}
          <button class="btn" type="button" data-action="open-admin-performance-detail" data-performance-id="${escapeAttr(editor.performanceId)}">返回详情</button>
        </div>
      </div>
    `;
  }

  async function submitAdminTicketConfig(editor) {
    const payload = validateTicketCategoryDraft(editor, { optional: false });
    if (!payload.valid) {
      toast(payload.message, "error");
      return;
    }
    const button = dialogRoot.querySelector("[data-admin-ticket-submit]");
    button.disabled = true;
    button.textContent = "保存中";
    try {
      await apiPut(
        `/api/performance/admin/${encodeURIComponent(editor.performanceId)}/shows/${encodeURIComponent(editor.showId)}/ticket-categories/config`,
        payload.data
      );
      toast("票档配置已保存", "success");
      await openAdminTicketConfig(editor.performanceId, editor.showId);
    } catch (error) {
      button.disabled = false;
      button.textContent = "保存票档配置";
      toast(apiMessage(error), "error");
    }
  }

  function renderAdminShowTicketDraft() {
    const node = dialogRoot.querySelector("[data-admin-show-ticket-editor]");
    if (!node || !state.adminShowTicketDraft) {
      return;
    }
    node.innerHTML = ticketDraftTemplate(state.adminShowTicketDraft);
    bindTicketDraftEditor(node, state.adminShowTicketDraft, renderAdminShowTicketDraft);
  }

  function createTicketDraft(options) {
    const seatMap = options.seatMap || venueSeatMapFromSeats(options.seats || [], "座位图");
    const seatMapSeats = Array.isArray(seatMap?.seats) ? seatMap.seats : [];
    const seats = Array.isArray(options.seats) ? options.seats : seatMapSeats;
    const categories = (options.categories || []).map(ticketCategoryDraftFromConfig);
    return {
      performanceId: normalizeId(options.performanceId),
      venueId: normalizeId(options.venueId),
      showId: normalizeId(options.showId),
      seatMapId: normalizeId(options.seatMapId || seatMap?.seatMapId),
      seatMap,
      seats,
      categories,
      activeIndex: categories.length ? 0 : -1,
      optional: Boolean(options.optional),
      readOnly: Boolean(options.readOnly),
      saleStatus: options.saleStatus || ""
    };
  }

  function ticketCategoryDraftFromConfig(config) {
    const seatIds = Array.isArray(config.seats)
      ? normalizeIdList(config.seats.map((seat) => seat.seatId))
      : Array.isArray(config.seatIds)
        ? normalizeIdList(config.seatIds)
        : [];
    const lockedSeatIds = Array.isArray(config.lockedSeats)
      ? normalizeIdList(config.lockedSeats.map((seat) => seat.seatId))
      : Array.isArray(config.lockedSeatIds)
        ? normalizeIdList(config.lockedSeatIds)
        : [];
    return {
      categoryName: config.categoryName || "",
      price: config.price == null ? "" : String(config.price),
      totalStock: config.totalStock == null ? "" : String(config.totalStock),
      seatSelectable: Number(config.seatSelectable ?? 1),
      seatIds,
      lockedSeatIds: lockedSeatIds.filter((seatId) => seatIds.includes(seatId)),
      lockMode: 0
    };
  }

  function newTicketCategoryDraft() {
    return {
      categoryName: "",
      price: "",
      totalStock: "",
      seatSelectable: 1,
      seatIds: [],
      lockedSeatIds: [],
      lockMode: 0
    };
  }

  function ticketDraftTemplate(draft) {
    const categories = draft.categories || [];
    const active = categories[draft.activeIndex] || null;
    return `
      <div class="ticket-config-head">
        <div>
          <h3>票档配置</h3>
          <p class="muted">${categories.length} 个票档 · ${ticketDraftSeatCount(categories)} 个已分配座位 · ${ticketDraftLockedSeatCount(categories)} 个锁定座位</p>
        </div>
        ${draft.readOnly ? "" : `<button class="btn compact primary" type="button" data-ticket-add>新增票档</button>`}
      </div>
      <div class="ticket-config-grid">
        <aside class="ticket-config-list" aria-label="票档列表">
          ${categories.length ? categories.map((category, index) => ticketDraftCategoryCard(category, index, draft.activeIndex)).join("") : inlineEmpty("暂无票档", "可新增票档后继续配置座位。")}
        </aside>
        <section class="ticket-config-workspace">
          ${active ? ticketDraftCategoryForm(draft, active, draft.activeIndex) : inlineEmpty("未选择票档", "新增或选择票档后继续。")}
        </section>
      </div>
    `;
  }

  function ticketDraftSeatCount(categories) {
    return (categories || []).reduce((sum, category) => sum + (Array.isArray(category.seatIds) ? category.seatIds.length : 0), 0);
  }

  function ticketDraftLockedSeatCount(categories) {
    return (categories || []).reduce((sum, category) => sum + (Array.isArray(category.lockedSeatIds) ? category.lockedSeatIds.length : 0), 0);
  }

  function ticketDraftCategoryCard(category, index, activeIndex) {
    const selected = index === activeIndex;
    const seatCount = Array.isArray(category.seatIds) ? category.seatIds.length : 0;
    const lockedCount = Array.isArray(category.lockedSeatIds) ? category.lockedSeatIds.length : 0;
    return `
      <button class="ticket-config-card ${selected ? "active" : ""}" type="button" data-ticket-index="${index}">
        <strong>${escapeHtml(category.categoryName || `票档 ${index + 1}`)}</strong>
        <span>${category.price ? formatMoney(category.price) : "未定价"} · 库存 ${escapeHtml(category.totalStock || "-")}</span>
        <span>${Number(category.seatSelectable) === 1 ? `可选座 ${seatCount} · 锁定 ${lockedCount}` : "不选座"}</span>
      </button>
    `;
  }

  function ticketDraftCategoryForm(draft, category, index) {
    const readOnly = draft.readOnly ? "disabled" : "";
    const selectedCount = Array.isArray(category.seatIds) ? category.seatIds.length : 0;
    const lockedCount = Array.isArray(category.lockedSeatIds) ? category.lockedSeatIds.length : 0;
    return `
      <div class="ticket-config-form">
        <div class="management-top">
          <h3>${escapeHtml(category.categoryName || `票档 ${index + 1}`)}</h3>
          ${draft.readOnly ? "" : `<button class="btn danger compact" type="button" data-ticket-remove>删除票档</button>`}
        </div>
        <div class="ticket-config-fields">
          <div class="field">
            <label>票档类型</label>
            <input data-ticket-field="categoryName" value="${escapeAttr(category.categoryName)}" maxlength="64" ${readOnly} required>
          </div>
          <div class="field">
            <label>价格</label>
            <input data-ticket-field="price" type="number" min="0.01" step="0.01" value="${escapeAttr(category.price)}" ${readOnly} required>
          </div>
          <div class="field">
            <label>库存</label>
            <input data-ticket-field="totalStock" type="number" min="1" step="1" value="${escapeAttr(category.totalStock)}" ${readOnly} required>
          </div>
          <div class="field">
            <label>选座方式</label>
            <select data-ticket-field="seatSelectable" ${readOnly}>
              ${option("1", "可选座", category.seatSelectable)}
              ${option("0", "不选座", category.seatSelectable)}
            </select>
          </div>
          <div class="field">
            <label>座位操作</label>
            <select data-ticket-field="lockMode" ${readOnly}>
              ${option("0", "分配座位", category.lockMode ?? 0)}
              ${option("1", "锁定座位", category.lockMode ?? 0)}
            </select>
          </div>
          <div class="field">
            <label>已选座位</label>
            <input value="${selectedCount}" disabled>
          </div>
          <div class="field">
            <label>锁定座位</label>
            <input value="${lockedCount}" disabled>
          </div>
        </div>
        ${Number(category.seatSelectable) === 1 ? ticketDraftSeatMapTemplate(draft, category) : ""}
      </div>
    `;
  }

  function ticketDraftSeatMapTemplate(draft, activeCategory) {
    const seatMap = draft.seatMap || venueSeatMapFromSeats(draft.seats || [], "座位图");
    const seats = Array.isArray(seatMap.seats) && seatMap.seats.length ? seatMap.seats : draft.seats || [];
    const byPosition = new Map(seats.map((seat) => [`${Number(seat.rowNo)}:${Number(seat.columnNo)}`, seat]));
    const rows = [];
    const rowCount = Number(seatMap.rowCount || 0);
    const columnCount = Number(seatMap.columnCount || 0);
    for (let row = 1; row <= rowCount; row += 1) {
      const cells = [];
      for (let column = 1; column <= columnCount; column += 1) {
        const seat = byPosition.get(`${row}:${column}`);
        cells.push(ticketDraftSeatButton(draft, activeCategory, seat));
      }
      rows.push(`<div class="seat-row" style="grid-template-columns: repeat(${Math.max(columnCount, 1)}, minmax(32px, 1fr));">${cells.join("")}</div>`);
    }
    return `
      <div class="seat-wrap ticket-seat-assignment">
        <div class="stage">舞台 / 银幕</div>
        <div class="seat-map">${rows.join("")}</div>
        <div class="legend">
          <span><i class="swatch selected"></i>当前票档</span>
          <span><i class="swatch locked"></i>锁定座位</span>
          <span><i class="swatch"></i>未分配</span>
          <span><i class="swatch other"></i>其他票档</span>
          <span><i class="swatch sold"></i>停用</span>
        </div>
      </div>
    `;
  }

  function ticketDraftSeatButton(draft, activeCategory, seat) {
    if (!seat) {
      return `<button type="button" class="seat missing" disabled aria-label="空位"></button>`;
    }
    const seatId = normalizeId(seat.seatId);
    const assignedIndex = ticketDraftAssignedCategoryIndex(draft, seatId);
    const activeAssigned = activeCategory.seatIds.includes(seatId);
    const activeLocked = activeAssigned && (activeCategory.lockedSeatIds || []).includes(seatId);
    const assignedOther = assignedIndex >= 0 && !activeAssigned;
    const enabled = Number(seat.status) === 1;
    const lockMode = Number(activeCategory.lockMode) === 1;
    const classNames = ["seat"];
    if (activeAssigned) classNames.push("selected");
    if (activeLocked) classNames.push("locked");
    if (assignedOther) classNames.push("other");
    if (!enabled) classNames.push("sold");
    const disabled = draft.readOnly || assignedOther || !enabled || (lockMode && !activeAssigned) ? "disabled" : "";
    const label = `${seat.seatNo || seatId} ${seat.areaName || ""}${activeLocked ? " 已锁定" : ""}`;
    return `
      <button type="button" class="${classNames.join(" ")}" data-ticket-seat="${escapeAttr(seatId)}" ${disabled} title="${escapeAttr(label)}" aria-label="${escapeAttr(label)}" aria-pressed="${activeAssigned ? "true" : "false"}">
        ${escapeHtml(shortSeatNo(seat.seatNo))}
      </button>
    `;
  }

  function ticketDraftAssignedCategoryIndex(draft, seatId) {
    const resolvedSeatId = normalizeId(seatId);
    return (draft.categories || []).findIndex((category) => (category.seatIds || []).includes(resolvedSeatId));
  }

  function bindTicketDraftEditor(root, draft, render) {
    if (!root || !draft) {
      return;
    }
    root.querySelectorAll("[data-ticket-index]").forEach((button) => {
      button.addEventListener("click", () => {
        draft.activeIndex = Number(button.dataset.ticketIndex);
        render();
      });
    });
    if (draft.readOnly) {
      return;
    }
    root.querySelector("[data-ticket-add]")?.addEventListener("click", () => {
      draft.categories.push(newTicketCategoryDraft());
      draft.activeIndex = draft.categories.length - 1;
      render();
    });
    root.querySelector("[data-ticket-remove]")?.addEventListener("click", () => {
      if (draft.activeIndex < 0) {
        return;
      }
      draft.categories.splice(draft.activeIndex, 1);
      draft.activeIndex = draft.categories.length ? Math.min(draft.activeIndex, draft.categories.length - 1) : -1;
      render();
    });
    root.querySelectorAll("[data-ticket-field]").forEach((field) => {
      const handler = () => {
        const category = draft.categories[draft.activeIndex];
        if (!category) {
          return;
        }
        const name = field.dataset.ticketField;
        if (name === "seatSelectable" || name === "lockMode") {
          category[name] = Number(field.value);
          if (name === "seatSelectable" && Number(field.value) === 0) {
            category.seatIds = [];
            category.lockedSeatIds = [];
            category.lockMode = 0;
          }
          render();
          return;
        }
        category[name] = field.value;
      };
      field.addEventListener(field.tagName === "SELECT" ? "change" : "input", handler);
    });
    root.querySelectorAll("[data-ticket-seat]").forEach((button) => {
      button.addEventListener("click", () => {
        const category = draft.categories[draft.activeIndex];
        if (!category) {
          return;
        }
        const seatId = normalizeId(button.dataset.ticketSeat);
        if (Number(category.lockMode) === 1) {
          if (!category.seatIds.includes(seatId)) {
            return;
          }
          const locked = new Set(category.lockedSeatIds || []);
          if (locked.has(seatId)) {
            locked.delete(seatId);
          } else {
            locked.add(seatId);
          }
          category.lockedSeatIds = [...locked].sort(compareIds);
          render();
          return;
        }
        const current = new Set(category.seatIds || []);
        if (current.has(seatId)) {
          current.delete(seatId);
          category.lockedSeatIds = (category.lockedSeatIds || []).filter((lockedSeatId) => lockedSeatId !== seatId);
        } else {
          current.add(seatId);
        }
        category.seatIds = [...current].sort(compareIds);
        category.totalStock = String(category.seatIds.length || "");
        render();
      });
    });
  }

  function validateTicketCategoryDraft(draft, options) {
    const optional = Boolean(options?.optional);
    if (!draft) {
      return optional ? { valid: true, data: [] } : { valid: false, message: "请配置票档" };
    }
    const categories = Array.isArray(draft.categories) ? draft.categories : [];
    if (!categories.length) {
      return optional ? { valid: true, data: [] } : { valid: false, message: "请至少新增一个票档" };
    }
    const seatMap = new Map((draft.seats || draft.seatMap?.seats || []).map((seat) => [normalizeId(seat.seatId), seat]));
    const seatMapId = normalizeId(draft.seatMapId || draft.seatMap?.seatMapId);
    const categoryNames = new Set();
    const assignedSeats = new Set();
    const data = [];
    for (const [index, category] of categories.entries()) {
      const displayName = `第 ${index + 1} 个票档`;
      const categoryName = String(category.categoryName || "").trim();
      const price = Number(category.price);
      const totalStock = Number(category.totalStock);
      const seatSelectable = Number(category.seatSelectable);
      if (!categoryName) return { valid: false, message: `${displayName}请输入票档类型` };
      if (categoryName.length > 64) return { valid: false, message: `${categoryName}票档类型不能超过 64 个字符` };
      const nameKey = categoryName.toLowerCase();
      if (categoryNames.has(nameKey)) return { valid: false, message: "票档类型不能重复" };
      categoryNames.add(nameKey);
      if (!Number.isFinite(price) || price <= 0) return { valid: false, message: `${categoryName}价格需要大于 0` };
      if (!Number.isInteger(totalStock) || totalStock <= 0) return { valid: false, message: `${categoryName}库存需要为正整数` };
      if (seatSelectable !== 0 && seatSelectable !== 1) return { valid: false, message: `${categoryName}请选择选座方式` };
      const seatIds = normalizeIdList(category.seatIds || []);
      const lockedSeatIds = normalizeIdList(category.lockedSeatIds || []).filter((seatId) => seatIds.includes(seatId));
      if (seatSelectable === 1) {
        if (!seatIds.length) return { valid: false, message: `${categoryName}请选择座位` };
        if (totalStock !== seatIds.length) return { valid: false, message: `${categoryName}库存需等于已选座位数` };
        for (const seatId of seatIds) {
          const seat = seatMap.get(seatId);
          if (!seat || Number(seat.status) !== 1) return { valid: false, message: `${categoryName}包含不可用座位` };
          if (assignedSeats.has(seatId)) return { valid: false, message: "同一座位只能配置到一个票档" };
          assignedSeats.add(seatId);
        }
      }
      const payload = {
        categoryName,
        price,
        totalStock,
        seatSelectable,
        seatIds: seatSelectable === 1 ? seatIds : [],
        lockedSeatIds: seatSelectable === 1 ? lockedSeatIds : []
      };
      if (/^\d+$/.test(seatMapId)) {
        payload.seatMapId = seatMapId;
      }
      data.push(payload);
    }
    return { valid: true, data };
  }

  async function loadAdminVenueSeatMap(venueId, name) {
    try {
      const seatMap = await apiGet(`/api/performance/admin/venues/${encodeURIComponent(venueId)}/seat-map`);
      if (seatMap) {
        return seatMap;
      }
    } catch (_error) {
      // Fall back while an old backend instance without the seat-map endpoint is still running.
    }
    const seats = await apiGet(`/api/performance/admin/venues/${encodeURIComponent(venueId)}/seats`);
    return venueSeatMapFromSeats(Array.isArray(seats) ? seats : [], name);
  }

  function venueSeatMapFromSeats(seats, name) {
    const records = Array.isArray(seats) ? seats : [];
    const rowCount = records.reduce((max, seat) => Math.max(max, Number(seat.rowNo || 0)), 0);
    const columnCount = records.reduce((max, seat) => Math.max(max, Number(seat.columnNo || 0)), 0);
    return {
      seatMapId: null,
      name: name ? `${name} 座位图` : "座位图",
      rowCount,
      columnCount,
      seats: records
    };
  }

  async function deleteAdminShow(performanceId, showId) {
    if (!requireLogin()) return;
    const resolvedPerformanceId = normalizeId(performanceId);
    const resolvedShowId = normalizeId(showId);
    if (!/^\d+$/.test(resolvedPerformanceId) || !/^\d+$/.test(resolvedShowId)) {
      toast("场次编号无效，无法删除", "error");
      return;
    }
    if (!window.confirm("确认删除该演出场次？删除后用户将不能再选择该场次。")) {
      return;
    }
    try {
      await apiDelete(`/api/performance/admin/${encodeURIComponent(resolvedPerformanceId)}/shows/${encodeURIComponent(resolvedShowId)}`);
      toast("场次已删除", "success");
      await openAdminPerformanceDetail(resolvedPerformanceId);
    } catch (error) {
      toast(apiMessage(error), "error");
    }
  }

  async function loadAdminShows(performanceId) {
    const sessions = await apiGet(`/api/performance/admin/${encodeURIComponent(normalizeId(performanceId))}/shows`);
    return Array.isArray(sessions) ? sessions : [];
  }

  async function loadAdminArtists() {
    if (!state.adminOptions.artists) {
      state.adminOptions.artists = await apiGet("/api/performance/admin/artists");
    }
    return Array.isArray(state.adminOptions.artists) ? state.adminOptions.artists : [];
  }

  async function loadAdminVenues() {
    if (!state.adminOptions.venues) {
      state.adminOptions.venues = await apiGet("/api/performance/admin/venues");
    }
    return Array.isArray(state.adminOptions.venues) ? state.adminOptions.venues : [];
  }

  function performanceTypeOptions(selected, includeAll = true) {
    const options = includeAll ? [option("", "全部", selected)] : [];
    [
      ["CONCERT", "演唱会"],
      ["MUSIC_FESTIVAL", "音乐节"],
      ["DRAMA", "戏剧"],
      ["EXHIBITION", "展览"],
      ["SPORTS", "体育赛事"]
    ].forEach(([value, label]) => options.push(option(value, label, selected)));
    return options.join("");
  }

  function adminVenueLabel(venue) {
    return `${venue.name || `场馆 ${venue.venueId}`} · ${formatVenueAddress(venue) || "-"}`;
  }

  function adminStatusPill(status) {
    return Number(status) === 1
      ? `<span class="pill ok">启用</span>`
      : `<span class="pill danger">停用</span>`;
  }

  function adminStatusText(status) {
    return Number(status) === 1 ? "启用" : "停用";
  }

  function isOnSale(item) {
    return String(item?.saleStatus || "").toUpperCase() === "ON_SALE";
  }

  function saleStatusText(status) {
    const value = String(status || "PENDING_SALE").toUpperCase();
    const map = {
      PENDING_SALE: "待开售",
      SCHEDULED: "定时开售",
      ON_SALE: "已开售"
    };
    return map[value] || value || "-";
  }

  function saleStatusPill(status) {
    const value = String(status || "PENDING_SALE").toUpperCase();
    const cls = value === "ON_SALE" ? "ok" : value === "SCHEDULED" ? "warn" : "";
    return `<span class="pill ${cls}">${escapeHtml(saleStatusText(value))}</span>`;
  }

  function saleStatusDescription(detail) {
    const value = String(detail?.saleStatus || "PENDING_SALE").toUpperCase();
    if (value === "ON_SALE") {
      return `已于 ${formatDateTime(detail?.actualSaleTime)} 开售`;
    }
    if (value === "SCHEDULED") {
      return `计划于 ${formatDateTime(detail?.scheduledSaleTime)} 开售`;
    }
    return "项目创建后默认待开售，开售前可调整场次、票档和锁座配置";
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
    state.adminSeatManager = null;
    state.adminTicketEditor = null;
    state.adminShowTicketDraft = null;
  }

  function setActionButtonsDisabled(disabled) {
    document.querySelectorAll("[data-create-order], [data-pay-create], [data-pay-success], [data-order-cancel], [data-user-order-cancel], [data-action='open-order-refund'], [data-action='open-refund-rollback']").forEach((button) => {
      button.disabled = disabled;
    });
  }

  function openOrderRefundDialog(orderId) {
    if (!requireLogin()) return;
    const resolvedOrderId = normalizeId(orderId);
    if (!/^\d+$/.test(resolvedOrderId)) {
      toast("订单号无效，无法申请退票", "error");
      return;
    }
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="order-refund-title">
          <div class="dialog-head">
            <h2 id="order-refund-title">申请退票</h2>
            <button class="btn compact" type="button" data-action="close-dialog">关闭</button>
          </div>
          <div class="dialog-body">
            <form id="order-refund-form" class="form-grid">
              <div class="field">
                <label for="order-refund-order-id">订单号</label>
                <input id="order-refund-order-id" name="orderId" value="${escapeAttr(resolvedOrderId)}" readonly required>
              </div>
              <div class="field">
                <label for="order-refund-reason">退票原因（选填）</label>
                <textarea id="order-refund-reason" name="reason" rows="4" maxlength="255" placeholder="如行程变更、无法按时观演等"></textarea>
              </div>
              <p class="form-error" data-order-refund-error hidden></p>
              <div class="button-row">
                <button class="btn danger" type="submit" data-order-refund-submit>提交退票申请</button>
                <button class="btn" type="button" data-action="close-dialog">取消</button>
              </div>
            </form>
          </div>
        </section>
      </div>
    `;
    const form = dialogRoot.querySelector("#order-refund-form");
    form.addEventListener("submit", submitOrderRefund);
    dialogRoot.querySelector("textarea").focus();
  }

  async function submitOrderRefund(event) {
    event.preventDefault();
    const form = event.target;
    const payload = validateOrderRefundForm(form);
    const errorNode = form.querySelector("[data-order-refund-error]");
    if (!payload.valid) {
      showFormError(errorNode, payload.message);
      return;
    }
    if (!window.confirm("确认提交退票申请？退款完成后电子票将失效，座位会释放。")) {
      return;
    }
    const submit = form.querySelector("[data-order-refund-submit]");
    submit.disabled = true;
    submit.textContent = "提交中";
    showFormError(errorNode, "");
    setActionButtonsDisabled(true);
    state.busy = true;
    try {
      const refund = await requestOrderRefund(payload.orderId, payload.reason);
      closeDialog();
      toast(refund?.orderStatus === "REFUNDED" ? "退票完成，退款已处理" : "退票申请已提交", "success");
      syncOrderAfterRefund(payload.orderId);
    } catch (error) {
      showFormError(errorNode, apiMessage(error));
      toast(apiMessage(error), "error");
      submit.disabled = false;
      submit.textContent = "提交退票申请";
    } finally {
      state.busy = false;
      setActionButtonsDisabled(false);
    }
  }

  function validateOrderRefundForm(form) {
    const data = Object.fromEntries(new FormData(form).entries());
    const orderId = normalizeId(data.orderId);
    const reason = String(data.reason || "").trim();
    if (!/^\d+$/.test(orderId)) {
      return { valid: false, message: "订单号必须为数字" };
    }
    if (reason.length > 255) {
      return { valid: false, message: "退票原因不能超过 255 个字符" };
    }
    return { valid: true, orderId, reason };
  }

  async function requestOrderRefund(orderId, reason) {
    return apiPost("/api/user/orders/refund", {
      orderId: normalizeId(orderId),
      reason
    });
  }

  function syncOrderAfterRefund(orderId) {
    const resolvedOrderId = normalizeId(orderId);
    const { path } = parseHash();
    if (path.startsWith("/checkout/") && normalizeId(path.split("/")[2]) === resolvedOrderId) {
      renderCheckout(resolvedOrderId);
      return;
    }
    if (path.startsWith("/tickets/") && normalizeId(path.split("/")[2]) === resolvedOrderId) {
      renderTickets(resolvedOrderId);
      return;
    }
    if (path === "/orders") {
      renderOrders();
    }
  }

  function openRefundRollbackDialog(orderId) {
    const resolvedOrderId = normalizeId(orderId);
    if (!/^\d+$/.test(resolvedOrderId)) {
      toast("订单号无效，无法发起退款回滚", "error");
      return;
    }
    dialogRoot.innerHTML = `
      <div class="dialog-backdrop">
        <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="refund-rollback-title">
          <div class="dialog-head">
            <h2 id="refund-rollback-title">退款回滚</h2>
            <button class="btn compact" type="button" data-action="close-dialog">关闭</button>
          </div>
          <div class="dialog-body">
            <form id="refund-rollback-form" class="form-grid">
              <div class="field">
                <label for="refund-rollback-order-id">订单号</label>
                <input id="refund-rollback-order-id" name="orderId" value="${escapeAttr(resolvedOrderId)}" readonly required>
              </div>
              <div class="field">
                <label for="refund-rollback-reason">回滚原因</label>
                <textarea id="refund-rollback-reason" name="reason" rows="4" maxlength="255" required placeholder="支付退款失败，恢复订单状态"></textarea>
              </div>
              <p class="form-error" data-refund-rollback-error hidden></p>
              <div class="button-row">
                <button class="btn danger" type="submit" data-refund-rollback-submit>提交回滚</button>
                <button class="btn" type="button" data-action="close-dialog">取消</button>
              </div>
            </form>
          </div>
        </section>
      </div>
    `;
    const form = dialogRoot.querySelector("#refund-rollback-form");
    form.addEventListener("submit", submitRefundRollback);
    dialogRoot.querySelector("textarea").focus();
  }

  async function submitRefundRollback(event) {
    event.preventDefault();
    const form = event.target;
    const payload = validateRefundRollbackForm(form);
    const errorNode = form.querySelector("[data-refund-rollback-error]");
    if (!payload.valid) {
      showFormError(errorNode, payload.message);
      return;
    }
    if (!window.confirm("确认提交退款回滚？")) {
      return;
    }
    const submit = form.querySelector("[data-refund-rollback-submit]");
    submit.disabled = true;
    submit.textContent = "提交中";
    showFormError(errorNode, "");
    setActionButtonsDisabled(true);
    state.busy = true;
    try {
      const order = await requestOrderRefundRollback(payload.orderId, payload.reason);
      closeDialog();
      toast("退款回滚已完成", "success");
      syncOrderAfterRefundRollback(order, payload.orderId);
    } catch (error) {
      showFormError(errorNode, apiMessage(error));
      toast(apiMessage(error), "error");
      submit.disabled = false;
      submit.textContent = "提交回滚";
    } finally {
      state.busy = false;
      setActionButtonsDisabled(false);
    }
  }

  function validateRefundRollbackForm(form) {
    const data = Object.fromEntries(new FormData(form).entries());
    const orderId = normalizeId(data.orderId);
    const reason = String(data.reason || "").trim();
    if (!/^\d+$/.test(orderId)) {
      return { valid: false, message: "订单号必须为数字" };
    }
    if (!reason) {
      return { valid: false, message: "请输入回滚原因" };
    }
    if (reason.length > 255) {
      return { valid: false, message: "回滚原因不能超过 255 个字符" };
    }
    return { valid: true, orderId, reason };
  }

  function showFormError(node, message) {
    if (!node) {
      return;
    }
    node.textContent = message || "";
    node.hidden = !message;
  }

  async function requestOrderRefundRollback(orderId, reason) {
    return apiPost("/api/order/refund/rollback", {
      orderId: normalizeId(orderId),
      reason
    });
  }

  function syncOrderAfterRefundRollback(order, fallbackOrderId) {
    const resolvedOrderId = resolveOrderId(order, fallbackOrderId);
    const { path } = parseHash();
    if (path.startsWith("/checkout/") && normalizeId(path.split("/")[2]) === resolvedOrderId) {
      app.innerHTML = checkoutTemplate(order, readContext(resolvedOrderId), state.checkoutPay[resolvedOrderId], resolvedOrderId);
      bindCheckout(order, resolvedOrderId);
      return;
    }
    if (path.startsWith("/tickets/") && normalizeId(path.split("/")[2]) === resolvedOrderId) {
      renderTickets(resolvedOrderId);
      return;
    }
    if (path === "/orders") {
      renderOrders();
    }
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
    return text
      .replace(/"([A-Za-z][A-Za-z0-9]*Id)"\s*:\s*(\d{16,})/g, '"$1":"$2"')
      .replace(/"([A-Za-z][A-Za-z0-9]*Ids)"\s*:\s*\[([^\]]*)\]/g, (_match, field, values) => {
        const safeValues = values.replace(/(^|,)\s*(\d{16,})\s*(?=,|$)/g, '$1"$2"');
        return `"${field}":[${safeValues}]`;
      });
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
    return path === "/orders"
      || path.startsWith("/checkout/")
      || path.startsWith("/tickets/")
      || path === "/account"
      || path.startsWith("/account/")
      || isAdminPerformanceRoute(path)
      || path.startsWith("/admin/");
  }

  function ensureSelection(showId, availability) {
    const key = String(showId);
    if (!state.detailSelection[key]) {
      const firstAvailable = availability.find((item) => Number(item.availableStock) > 0) || availability[0];
      state.detailSelection[key] = {
        categoryId: normalizeId(firstAvailable?.categoryId) || null,
        selectedSeats: [],
        quantity: 1
      };
    }
    const selection = state.detailSelection[key];
    selection.categoryId = normalizeId(selection.categoryId);
    selection.selectedSeats = normalizeIdList(selection.selectedSeats);
    const categoryExists = availability.some((item) => sameId(item.categoryId, selection.categoryId));
    if (!categoryExists) {
      selection.categoryId = normalizeId(availability[0]?.categoryId) || null;
      selection.selectedSeats = [];
      selection.quantity = 1;
    }
    return selection;
  }

  function selectedSeatDetails(seats, seatIds) {
    const map = new Map((seats || []).map((seat) => [normalizeId(seat.seatId), seat]));
    return normalizeIdList(seatIds || []).map((id) => map.get(id)).filter(Boolean);
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

  function sameId(left, right) {
    const normalizedLeft = normalizeId(left);
    return normalizedLeft !== "" && normalizedLeft === normalizeId(right);
  }

  function normalizeIdList(values) {
    if (!Array.isArray(values)) {
      return [];
    }
    return [...new Set(values.map(normalizeId).filter((value) => /^\d+$/.test(value)))];
  }

  function compareIds(left, right) {
    const a = normalizeId(left);
    const b = normalizeId(right);
    if (/^\d+$/.test(a) && /^\d+$/.test(b) && a.length !== b.length) {
      return a.length - b.length;
    }
    return a.localeCompare(b);
  }

  function checkoutHash(orderId) {
    return `#/checkout/${encodeURIComponent(normalizeId(orderId))}`;
  }

  function ticketsHash(orderId) {
    return `#/tickets/${encodeURIComponent(normalizeId(orderId))}`;
  }

  function seatLabelFromContext(context, seatId) {
    const seat = context?.seats?.find((item) => sameId(item.seatId, seatId));
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
    } else if (isAdminPerformanceRoute(path) || path.startsWith("/admin/")) {
      document.querySelector('[data-nav="admin"]')?.classList.add("active");
    } else {
      document.querySelector('[data-nav="home"]')?.classList.add("active");
    }
  }

  function isAdminPerformanceRoute(path) {
    const normalized = String(path || "").replace(/\/+$/, "") || "/";
    return normalized === "/admin" || normalized === "/admin/performances";
  }

  function isAdminArtistRoute(path) {
    const normalized = String(path || "").replace(/\/+$/, "") || "/";
    return normalized === "/admin/artists";
  }

  function isAdminVenueRoute(path) {
    const normalized = String(path || "").replace(/\/+$/, "") || "/";
    return normalized === "/admin/venues";
  }

  function adminTabs(active) {
    const tabs = [
      ["performances", "#/admin/performances", "演出项目"],
      ["artists", "#/admin/artists", "艺人/团队"],
      ["venues", "#/admin/venues", "场馆"]
    ];
    return `
      <nav class="admin-tabs" aria-label="后台基础资料管理">
        ${tabs.map(([key, href, label]) => `<a href="${href}" class="${key === active ? "active" : ""}">${label}</a>`).join("")}
      </nav>
    `;
  }

  function matchesKeyword(values, keyword) {
    const query = String(keyword || "").trim().toLowerCase();
    if (!query) {
      return true;
    }
    return values.some((value) => String(value || "").toLowerCase().includes(query));
  }

  function option(value, label, selected) {
    return `<option value="${escapeAttr(value)}" ${String(value) === String(selected) ? "selected" : ""}>${escapeHtml(label)}</option>`;
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
      MUSIC_FESTIVAL: "音乐节",
      DRAMA: "戏剧",
      EXHIBITION: "展览",
      SPORTS: "体育赛事",
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
      REFUNDING: "退款中",
      REFUNDED: "已退款",
      CANCELED: "已取消",
      CLOSED: "已关闭",
      WAITING: "待支付",
      SUCCESS: "成功"
    };
    return map[status] || status || "-";
  }

  function ticketUnavailableState(status) {
    const map = {
      REFUNDING: {
        title: "退票处理中",
        description: "退款处理完成后电子票会失效，座位释放结果以订单状态为准。"
      },
      REFUNDED: {
        title: "已退票",
        description: "退款已处理，电子票已失效。"
      },
      CANCELED: {
        title: "订单已取消",
        description: "该订单未完成支付，未生成电子票。"
      },
      CLOSED: {
        title: "订单已关闭",
        description: "该订单已关闭，未生成可用电子票。"
      }
    };
    return map[status] || {
      title: "尚未出票",
      description: "订单支付成功后会生成电子票。"
    };
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
    const cls = status === "PAID" || status === "SUCCESS" ? "ok" : status === "PENDING_PAYMENT" || status === "WAITING" || status === "REFUNDING" ? "warn" : "danger";
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

  function formatDuration(minutes) {
    const value = Number(minutes);
    if (!Number.isFinite(value) || value <= 0) {
      return "-";
    }
    const rounded = Math.round(value);
    const hours = Math.floor(rounded / 60);
    const rest = rounded % 60;
    if (hours > 0 && rest > 0) {
      return `${hours}小时${rest}分钟`;
    }
    if (hours > 0) {
      return `${hours}小时`;
    }
    return `${rest}分钟`;
  }

  function toDateTimeLocalValue(value) {
    if (!value) {
      return "";
    }
    const text = String(value).trim().replace(" ", "T");
    const match = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.exec(text);
    if (match) {
      return match[0];
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return "";
    }
    return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}T${pad2(date.getHours())}:${pad2(date.getMinutes())}`;
  }

  function normalizeDateTimeInput(value) {
    const text = String(value || "").trim();
    return text.length === 16 ? `${text}:00` : text;
  }

  function pad2(value) {
    return String(value).padStart(2, "0");
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
