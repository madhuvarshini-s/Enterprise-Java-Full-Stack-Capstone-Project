/**
 * APEX FINANCIAL SERVICES — CLIENT CONTROLLER & STATE MANAGER
 * Single-Page Application Client Logic
 */

class ApexApp {
    constructor() {
        this.token = localStorage.getItem('apex_token') || null;
        this.user = JSON.parse(localStorage.getItem('apex_user') || 'null');
        this.accounts = [];
        this.selectedAccountFilter = null;
        this.transactions = [];
        this.activeLedgerType = 'ALL';
        this.searchQuery = '';
        
        // Admin state
        this.adminAccounts = [];
        this.adminTransactions = [];

        // Bindings
        this.init();
    }

    init() {
        if (this.token && this.user) {
            this.setupAuthenticatedSession();
        } else {
            this.showAuthView();
        }
    }

    // =========================================================================
    // HTTP API CLIENT HELPER
    // =========================================================================
    async apiRequest(url, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...(options.headers || {})
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        try {
            const response = await fetch(url, { ...options, headers });
            
            // Handle 401 Unauthorized (expired token)
            if (response.status === 401) {
                this.showToast('Your session has expired. Please sign in again.', 'error');
                this.logout();
                throw new Error('Unauthorized');
            }

            const data = await response.json().catch(() => null);

            if (!response.ok) {
                let errorMsg = 'An unexpected error occurred.';
                if (data && data.message) {
                    errorMsg = data.message;
                } else if (data && data.fieldErrors && data.fieldErrors.length > 0) {
                    errorMsg = data.fieldErrors.map(f => `${f.field}: ${f.message}`).join(', ');
                } else if (data && data.error) {
                    errorMsg = data.error;
                }
                throw new Error(errorMsg);
            }

            return data;
        } catch (error) {
            throw error;
        }
    }

    // =========================================================================
    // AUTHENTICATION & SESSION
    // =========================================================================
    async handleLogin(event) {
        event.preventDefault();
        const username = document.getElementById('login-username').value.trim();
        const password = document.getElementById('login-password').value;
        const btn = document.getElementById('login-submit-btn');

        btn.disabled = true;
        btn.innerHTML = '<span>Verifying credentials...</span>';

        try {
            const data = await this.apiRequest('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ username, password })
            });

            this.token = data.token;
            this.user = {
                id: data.id,
                username: data.username,
                email: data.email,
                fullName: data.fullName,
                roles: data.roles
            };

            localStorage.setItem('apex_token', this.token);
            localStorage.setItem('apex_user', JSON.stringify(this.user));

            this.showToast(`Welcome back, ${data.fullName}!`, 'success');
            this.setupAuthenticatedSession();
        } catch (error) {
            this.showToast(error.message, 'error');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<span>Authenticate Securely</span><i data-lucide="arrow-right"></i>';
            lucide.createIcons();
        }
    }

    async quickLogin(username, password) {
        document.getElementById('login-username').value = username;
        document.getElementById('login-password').value = password;
        this.switchAuthTab('login');
        const form = document.getElementById('login-form');
        form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
    }

    async handleRegister(event) {
        event.preventDefault();
        const username = document.getElementById('reg-username').value.trim();
        const fullName = document.getElementById('reg-fullname').value.trim();
        const email = document.getElementById('reg-email').value.trim();
        const phoneNumber = document.getElementById('reg-phone').value.trim();
        const password = document.getElementById('reg-password').value;
        const btn = document.getElementById('register-submit-btn');

        btn.disabled = true;
        btn.innerHTML = '<span>Provisioning Account...</span>';

        try {
            const response = await this.apiRequest('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify({ username, fullName, email, phoneNumber, password })
            });

            this.showToast(response.message || 'Registration successful! Opening account...', 'success');
            // Auto login with new credentials
            await this.quickLogin(username, password);
        } catch (error) {
            this.showToast(error.message, 'error');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<span>Create Account & Claim Credit</span><i data-lucide="check-circle"></i>';
            lucide.createIcons();
        }
    }

    logout() {
        this.token = null;
        this.user = null;
        this.accounts = [];
        this.transactions = [];
        localStorage.removeItem('apex_token');
        localStorage.removeItem('apex_user');
        this.showAuthView();
        this.showToast('You have been signed out safely.', 'info');
    }

    switchAuthTab(tab) {
        const loginTab = document.getElementById('tab-login-btn');
        const regTab = document.getElementById('tab-register-btn');
        const loginForm = document.getElementById('login-form');
        const regForm = document.getElementById('register-form');

        if (tab === 'login') {
            loginTab.classList.add('active');
            regTab.classList.remove('active');
            loginForm.style.display = 'flex';
            regForm.style.display = 'none';
        } else {
            regTab.classList.add('active');
            loginTab.classList.remove('active');
            regForm.style.display = 'flex';
            loginForm.style.display = 'none';
        }
        lucide.createIcons();
    }

    showAuthView() {
        document.getElementById('auth-view').style.display = 'block';
        document.getElementById('customer-view').style.display = 'none';
        document.getElementById('admin-view').style.display = 'none';
        document.getElementById('nav-links').style.display = 'none';
        document.getElementById('user-pill').style.display = 'none';
    }

    setupAuthenticatedSession() {
        document.getElementById('auth-view').style.display = 'none';
        document.getElementById('nav-links').style.display = 'flex';
        document.getElementById('user-pill').style.display = 'flex';

        // Set user badge details
        document.getElementById('user-display-name').textContent = this.user.fullName || this.user.username;
        const initials = (this.user.fullName || this.user.username)
            .split(' ')
            .map(n => n[0])
            .join('')
            .substring(0, 2)
            .toUpperCase();
        document.getElementById('user-avatar').textContent = initials;

        const isAdmin = this.user.roles && this.user.roles.includes('ROLE_ADMIN');
        const badge = document.getElementById('user-role-badge');
        badge.textContent = isAdmin ? 'SYSTEM ADMIN' : 'CUSTOMER';
        badge.style.color = isAdmin ? 'var(--purple)' : 'var(--emerald)';

        // Show admin tab if admin
        const adminBtn = document.getElementById('nav-admin-btn');
        adminBtn.style.display = isAdmin ? 'inline-flex' : 'none';

        // Load customer dashboard by default
        this.showCustomerDashboard();
    }

    // =========================================================================
    // VIEW SWITCHING
    // =========================================================================
    showCustomerDashboard() {
        document.getElementById('customer-view').style.display = 'block';
        document.getElementById('admin-view').style.display = 'none';
        document.getElementById('nav-dashboard-btn').classList.add('active');
        document.getElementById('nav-admin-btn').classList.remove('active');

        document.getElementById('dash-customer-name').textContent = this.user.fullName || this.user.username;

        this.loadCustomerData();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    showAdminConsole() {
        document.getElementById('customer-view').style.display = 'none';
        document.getElementById('admin-view').style.display = 'block';
        document.getElementById('nav-admin-btn').classList.add('active');
        document.getElementById('nav-dashboard-btn').classList.remove('active');

        this.refreshAdminData();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    scrollToLedger() {
        const ledger = document.getElementById('ledger-section');
        if (ledger) {
            ledger.scrollIntoView({ behavior: 'smooth' });
        }
    }

    // =========================================================================
    // CUSTOMER DATA LOADING & RENDERING
    // =========================================================================
    async loadCustomerData() {
        await this.fetchAccounts();
        await this.fetchTransactions();
    }

    async fetchAccounts() {
        try {
            const data = await this.apiRequest('/api/accounts/my-accounts');
            this.accounts = data || [];
            this.renderAccountsOverview();
            this.populateModalSelects();
        } catch (error) {
            this.showToast('Failed to load accounts: ' + error.message, 'error');
        }
    }

    renderAccountsOverview() {
        // Calculate total balance
        const total = this.accounts.reduce((acc, curr) => {
            return acc + (curr.status === 'ACTIVE' ? parseFloat(curr.balance) : 0);
        }, 0);

        document.getElementById('dash-total-balance').textContent = this.formatCurrency(total);
        document.getElementById('dash-accounts-count').textContent = this.accounts.length;

        const grid = document.getElementById('accounts-grid');
        grid.innerHTML = '';

        if (this.accounts.length === 0) {
            grid.innerHTML = `
                <div class="empty-state" style="grid-column: 1/-1; padding: 2rem;">
                    <i data-lucide="wallet"></i>
                    <p>No active accounts found. Open a new checking or savings account.</p>
                </div>
            `;
            lucide.createIcons();
            return;
        }

        this.accounts.forEach(acc => {
            const card = document.createElement('div');
            const isSelected = this.selectedAccountFilter === acc.accountNumber;
            card.className = `bank-account-card ${isSelected ? 'selected' : ''}`;
            card.onclick = () => this.toggleAccountFilter(acc.accountNumber);

            const typeClass = (acc.accountType || '').toLowerCase();
            const statusClass = (acc.status || '').toLowerCase();

            card.innerHTML = `
                <div class="acc-card-top">
                    <span class="acc-type-badge ${typeClass}">${acc.accountType}</span>
                    <span class="acc-status-indicator ${statusClass}">
                        <span class="status-dot"></span>
                        ${acc.status}
                    </span>
                </div>
                <div class="acc-number-row">
                    <span>${acc.accountNumber}</span>
                    <button class="acc-copy-btn" onclick="event.stopPropagation(); app.copyToClipboard('${acc.accountNumber}')" title="Copy Account Number">
                        <i data-lucide="copy" style="width: 14px; height: 14px;"></i>
                    </button>
                </div>
                <div>
                    <div class="acc-balance-label">Available Balance</div>
                    <div class="acc-balance-value">${this.formatCurrency(acc.balance)}</div>
                </div>
            `;
            grid.appendChild(card);
        });

        lucide.createIcons();
    }

    toggleAccountFilter(accNumber) {
        if (this.selectedAccountFilter === accNumber) {
            this.selectedAccountFilter = null;
            document.getElementById('ledger-filter-status').textContent = 'Showing all recent account activities';
        } else {
            this.selectedAccountFilter = accNumber;
            document.getElementById('ledger-filter-status').textContent = `Filtered to account: ${accNumber}`;
        }
        this.renderAccountsOverview();
        this.fetchTransactions();
    }

    async fetchTransactions() {
        try {
            let url = '/api/transactions/recent?limit=25';
            if (this.selectedAccountFilter) {
                url = `/api/transactions/history/${this.selectedAccountFilter}`;
            }

            const data = await this.apiRequest(url);
            this.transactions = data || [];
            document.getElementById('dash-monthly-txns').textContent = this.transactions.length;
            this.filterTransactions();
        } catch (error) {
            this.showToast('Failed to fetch transactions: ' + error.message, 'error');
        }
    }

    filterTransactions() {
        const query = (document.getElementById('ledger-search-input')?.value || '').toLowerCase().trim();
        const typeFilter = this.activeLedgerType;

        const filtered = this.transactions.filter(txn => {
            // Type Match
            const matchesType = (typeFilter === 'ALL') || (txn.transactionType === typeFilter);

            // Search Query Match
            const matchesQuery = !query ||
                (txn.referenceNumber && txn.referenceNumber.toLowerCase().includes(query)) ||
                (txn.description && txn.description.toLowerCase().includes(query)) ||
                (txn.sourceAccountNumber && txn.sourceAccountNumber.toLowerCase().includes(query)) ||
                (txn.targetAccountNumber && txn.targetAccountNumber.toLowerCase().includes(query));

            return matchesType && matchesQuery;
        });

        this.renderTransactionsTable(filtered);
    }

    setLedgerTypeFilter(type) {
        this.activeLedgerType = type;
        const pills = document.querySelectorAll('.filter-pill');
        pills.forEach(pill => {
            if (pill.getAttribute('data-type') === type) {
                pill.classList.add('active');
            } else {
                pill.classList.remove('active');
            }
        });
        this.filterTransactions();
    }

    renderTransactionsTable(items) {
        const tbody = document.getElementById('transactions-tbody');
        const emptyState = document.getElementById('transactions-empty-state');
        tbody.innerHTML = '';

        if (!items || items.length === 0) {
            emptyState.style.display = 'block';
            return;
        }

        emptyState.style.display = 'none';

        items.forEach(txn => {
            const tr = document.createElement('tr');
            
            // Format type badge
            const typeLower = (txn.transactionType || '').toLowerCase();
            const badgeClass = `badge-tag ${typeLower}`;

            // Amount formatting (+ for deposit/inward, - for withdrawal/transfer outward)
            let isPositive = txn.transactionType === 'DEPOSIT';
            let formattedAmount = this.formatCurrency(txn.amount);
            let amountClass = isPositive ? 'amount-positive' : 'amount-negative';
            let prefix = isPositive ? '+' : '-';

            tr.innerHTML = `
                <td>${this.formatDate(txn.timestamp)}</td>
                <td><span class="ref-code">${txn.referenceNumber}</span></td>
                <td><span class="${badgeClass}">${txn.transactionType}</span></td>
                <td>${txn.description || 'N/A'}</td>
                <td>
                    <span style="font-family: monospace; font-size: 0.8rem;">
                        ${txn.sourceAccountNumber} &rarr; ${txn.targetAccountNumber}
                    </span>
                </td>
                <td class="${amountClass}">${prefix}${formattedAmount}</td>
                <td style="font-weight: 600;">${this.formatCurrency(txn.postBalance)}</td>
                <td>
                    <span class="status-badge-completed">
                        <i data-lucide="check-circle" style="width: 14px; height: 14px; display: inline; vertical-align: -2px;"></i>
                        ${txn.status}
                    </span>
                </td>
            `;
            tbody.appendChild(tr);
        });

        lucide.createIcons();
    }

    // =========================================================================
    // MODAL HANDLERS (TRANSFER, DEPOSIT, WITHDRAW, NEW ACCOUNT)
    // =========================================================================
    populateModalSelects() {
        const transferSelect = document.getElementById('transfer-source-acc');
        const depositSelect = document.getElementById('deposit-acc');
        const withdrawSelect = document.getElementById('withdraw-acc');

        const activeAccounts = this.accounts.filter(a => a.status === 'ACTIVE');

        const optionsHtml = activeAccounts.map(acc => `
            <option value="${acc.accountNumber}" data-balance="${acc.balance}">
                ${acc.accountNumber} (${acc.accountType} - ${this.formatCurrency(acc.balance)})
            </option>
        `).join('');

        if (transferSelect) transferSelect.innerHTML = optionsHtml;
        if (depositSelect) depositSelect.innerHTML = optionsHtml;
        if (withdrawSelect) withdrawSelect.innerHTML = optionsHtml;

        this.updateTransferSourceMax();
    }

    openTransferModal() {
        this.populateModalSelects();
        document.getElementById('transfer-target-acc').value = '';
        document.getElementById('transfer-amount').value = '';
        document.getElementById('transfer-desc').value = '';
        document.getElementById('recipient-lookup-result').style.display = 'none';
        this.openModal('transfer-modal');
    }

    openDepositModal() {
        this.populateModalSelects();
        document.getElementById('deposit-amount').value = '';
        document.getElementById('deposit-desc').value = '';
        this.openModal('deposit-modal');
    }

    openWithdrawModal() {
        this.populateModalSelects();
        document.getElementById('withdraw-amount').value = '';
        document.getElementById('withdraw-desc').value = '';
        this.openModal('withdraw-modal');
    }

    openNewAccountModal() {
        document.getElementById('new-acc-deposit').value = '500.00';
        this.openModal('new-account-modal');
    }

    openModal(modalId) {
        document.getElementById(modalId)?.classList.add('active');
        lucide.createIcons();
    }

    closeModal(modalId) {
        document.getElementById(modalId)?.classList.remove('active');
    }

    updateTransferSourceMax() {
        const select = document.getElementById('transfer-source-acc');
        const helper = document.getElementById('transfer-available-balance');
        if (!select || !select.options[select.selectedIndex]) return;

        const balance = select.options[select.selectedIndex].getAttribute('data-balance');
        if (helper && balance) {
            helper.textContent = `Available: ${this.formatCurrency(balance)}`;
        }
    }

    async lookupRecipientName() {
        const targetAcc = document.getElementById('transfer-target-acc').value.trim();
        const resultCard = document.getElementById('recipient-lookup-result');
        const nameSpan = document.getElementById('lookup-recipient-name');
        const metaSpan = document.getElementById('lookup-recipient-meta');

        if (!targetAcc) {
            resultCard.style.display = 'none';
            return;
        }

        try {
            const data = await this.apiRequest(`/api/accounts/recipient/lookup?accountNumber=${encodeURIComponent(targetAcc)}`);
            nameSpan.textContent = data.ownerFullName || data.ownerUsername;
            metaSpan.textContent = `${data.accountType} • ${data.status}`;
            resultCard.style.display = 'flex';
            lucide.createIcons();
        } catch (error) {
            resultCard.style.display = 'none';
            this.showToast('Recipient lookup: ' + error.message, 'error');
        }
    }

    async handleTransferSubmit(event) {
        event.preventDefault();
        const sourceAccountNumber = document.getElementById('transfer-source-acc').value;
        const targetAccountNumber = document.getElementById('transfer-target-acc').value.trim();
        const amount = parseFloat(document.getElementById('transfer-amount').value);
        const description = document.getElementById('transfer-desc').value.trim();
        const btn = document.getElementById('transfer-submit-btn');

        btn.disabled = true;
        btn.innerHTML = '<span>Processing Transfer...</span>';

        try {
            const response = await this.apiRequest('/api/transactions/transfer', {
                method: 'POST',
                body: JSON.stringify({ sourceAccountNumber, targetAccountNumber, amount, description })
            });

            this.showToast(`Transfer of ${this.formatCurrency(amount)} completed! Ref: ${response.referenceNumber}`, 'success');
            this.closeModal('transfer-modal');
            await this.loadCustomerData();
        } catch (error) {
            this.showToast(error.message, 'error');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<span>Execute Transfer</span><i data-lucide="arrow-right"></i>';
            lucide.createIcons();
        }
    }

    async handleDepositSubmit(event) {
        event.preventDefault();
        const accountNumber = document.getElementById('deposit-acc').value;
        const amount = parseFloat(document.getElementById('deposit-amount').value);
        const description = document.getElementById('deposit-desc').value.trim();
        const btn = document.getElementById('deposit-submit-btn');

        btn.disabled = true;
        btn.innerHTML = '<span>Crediting Account...</span>';

        try {
            const response = await this.apiRequest('/api/transactions/deposit', {
                method: 'POST',
                body: JSON.stringify({ accountNumber, amount, description })
            });

            this.showToast(`Deposited ${this.formatCurrency(amount)} into ${accountNumber}!`, 'success');
            this.closeModal('deposit-modal');
            await this.loadCustomerData();
        } catch (error) {
            this.showToast(error.message, 'error');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<span>Confirm Deposit</span><i data-lucide="check"></i>';
            lucide.createIcons();
        }
    }

    async handleWithdrawSubmit(event) {
        event.preventDefault();
        const accountNumber = document.getElementById('withdraw-acc').value;
        const amount = parseFloat(document.getElementById('withdraw-amount').value);
        const description = document.getElementById('withdraw-desc').value.trim();
        const btn = document.getElementById('withdraw-submit-btn');

        btn.disabled = true;
        btn.innerHTML = '<span>Processing Debit...</span>';

        try {
            const response = await this.apiRequest('/api/transactions/withdraw', {
                method: 'POST',
                body: JSON.stringify({ accountNumber, amount, description })
            });

            this.showToast(`Withdrew ${this.formatCurrency(amount)} from ${accountNumber}!`, 'success');
            this.closeModal('withdraw-modal');
            await this.loadCustomerData();
        } catch (error) {
            this.showToast(error.message, 'error');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<span>Confirm Withdrawal</span><i data-lucide="check"></i>';
            lucide.createIcons();
        }
    }

    async handleNewAccountSubmit(event) {
        event.preventDefault();
        const accountType = document.getElementById('new-acc-type').value;
        const initialDeposit = parseFloat(document.getElementById('new-acc-deposit').value || 0);
        const btn = document.getElementById('new-acc-submit-btn');

        btn.disabled = true;
        btn.innerHTML = '<span>Opening Account...</span>';

        try {
            const response = await this.apiRequest('/api/accounts/create', {
                method: 'POST',
                body: JSON.stringify({ accountType, initialDeposit })
            });

            this.showToast(`New ${accountType} Account ${response.accountNumber} created successfully!`, 'success');
            this.closeModal('new-account-modal');
            await this.loadCustomerData();
        } catch (error) {
            this.showToast(error.message, 'error');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<span>Provision Account</span><i data-lucide="check"></i>';
            lucide.createIcons();
        }
    }

    // =========================================================================
    // ADMIN OPERATIONS
    // =========================================================================
    async refreshAdminData() {
        try {
            // 1. Dashboard Stats
            const stats = await this.apiRequest('/api/admin/dashboard');
            document.getElementById('kpi-liquidity').textContent = this.formatCurrency(stats.totalLiquidity);
            document.getElementById('kpi-volume').textContent = this.formatCurrency(stats.totalVolume);
            document.getElementById('kpi-total-accounts').textContent = stats.totalAccounts;
            document.getElementById('kpi-active-accounts').textContent = `${stats.activeAccounts} Active`;
            document.getElementById('kpi-frozen-accounts').textContent = `${stats.frozenAccounts} Frozen`;
            document.getElementById('kpi-users').textContent = stats.totalUsers;

            // 2. All Bank Accounts
            this.adminAccounts = await this.apiRequest('/api/admin/accounts');
            document.getElementById('admin-accounts-count').textContent = `${this.adminAccounts.length} Total Accounts`;
            this.renderAdminAccounts();

            // 3. Global Transaction Stream
            this.adminTransactions = await this.apiRequest('/api/admin/transactions?limit=30');
            this.renderAdminTransactions();
        } catch (error) {
            this.showToast('Admin sync failed: ' + error.message, 'error');
        }
    }

    renderAdminAccounts() {
        const tbody = document.getElementById('admin-accounts-tbody');
        tbody.innerHTML = '';

        this.adminAccounts.forEach(acc => {
            const tr = document.createElement('tr');
            const isFrozen = acc.status === 'FROZEN';

            tr.innerHTML = `
                <td><span class="ref-code">${acc.accountNumber}</span></td>
                <td>
                    <strong>${acc.ownerFullName}</strong>
                    <div style="font-size: 0.72rem; color: var(--text-faint);">@${acc.ownerUsername}</div>
                </td>
                <td><span class="acc-type-badge ${(acc.accountType || '').toLowerCase()}">${acc.accountType}</span></td>
                <td style="font-weight: 700;">${this.formatCurrency(acc.balance)}</td>
                <td>
                    <span class="${isFrozen ? 'status-badge-frozen' : 'status-badge-completed'}">
                        ${acc.status}
                    </span>
                </td>
                <td>
                    <button class="toggle-status-btn ${isFrozen ? 'unfreeze' : 'freeze'}"
                            onclick="app.toggleAccountStatus('${acc.accountNumber}', '${acc.status}')">
                        ${isFrozen ? 'Unfreeze Account' : 'Freeze Account'}
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    renderAdminTransactions() {
        const tbody = document.getElementById('admin-transactions-tbody');
        tbody.innerHTML = '';

        this.adminTransactions.forEach(txn => {
            const tr = document.createElement('tr');
            const typeLower = (txn.transactionType || '').toLowerCase();

            tr.innerHTML = `
                <td style="font-size: 0.78rem;">${this.formatDate(txn.timestamp)}</td>
                <td><span class="ref-code">${txn.referenceNumber}</span></td>
                <td><span class="badge-tag ${typeLower}">${txn.transactionType}</span></td>
                <td style="font-weight: 700;">${this.formatCurrency(txn.amount)}</td>
                <td style="font-family: monospace; font-size: 0.78rem;">
                    ${txn.sourceAccountNumber} &rarr; ${txn.targetAccountNumber}
                </td>
                <td>
                    <span class="status-badge-completed">${txn.status}</span>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    async toggleAccountStatus(accountNumber, currentStatus) {
        const nextStatus = currentStatus === 'ACTIVE' ? 'FROZEN' : 'ACTIVE';
        try {
            await this.apiRequest(`/api/admin/accounts/${accountNumber}/status`, {
                method: 'PUT',
                body: JSON.stringify({ status: nextStatus })
            });

            this.showToast(`Account ${accountNumber} has been updated to ${nextStatus}.`, 'success');
            await this.refreshAdminData();
        } catch (error) {
            this.showToast('Status update failed: ' + error.message, 'error');
        }
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================
    formatCurrency(amount) {
        const num = parseFloat(amount || 0);
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(num);
    }

    formatDate(isoString) {
        if (!isoString) return 'N/A';
        const d = new Date(isoString);
        return d.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    async copyToClipboard(text) {
        try {
            await navigator.clipboard.writeText(text);
            this.showToast(`Account number ${text} copied to clipboard!`, 'info');
        } catch (err) {
            this.showToast(`Account: ${text}`, 'info');
        }
    }

    showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;

        let iconName = 'info';
        if (type === 'success') iconName = 'check-circle';
        if (type === 'error') iconName = 'alert-triangle';

        toast.innerHTML = `
            <i data-lucide="${iconName}"></i>
            <span>${message}</span>
        `;

        container.appendChild(toast);
        lucide.createIcons();

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }
}

// Global App Instance
const app = new ApexApp();
