package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.book.BookEntryResponse
import com.ssafy.seveniTax.data.repository.BookEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EntryFilter(val label: String) {
    ALL("전체"), INCOME("수입"), EXPENSE("비용"), ASSET("자산")
}

enum class BookTab(val label: String) {
    LIST("장부 목록"), CATEGORY("세목별")
}

@HiltViewModel
class BookEntryViewModel @Inject constructor(
    private val bookEntryRepository: BookEntryRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<BookEntryResponse>>(emptyList())
    val entries: StateFlow<List<BookEntryResponse>> = _entries.asStateFlow()

    private val _unconfirmedCount = MutableStateFlow(3) // TODO: 데모용 더미값, 추후 0으로 복원
    val unconfirmedCount: StateFlow<Int> = _unconfirmedCount.asStateFlow()

    private val _selectedYear = MutableStateFlow(2025)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(3)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedFilter = MutableStateFlow(EntryFilter.ALL)
    val selectedFilter: StateFlow<EntryFilter> = _selectedFilter.asStateFlow()

    private val _selectedTab = MutableStateFlow(BookTab.LIST)
    val selectedTab: StateFlow<BookTab> = _selectedTab.asStateFlow()

    // 필터 설정
    private val _filterCategory = MutableStateFlow("전체")
    val filterCategory: StateFlow<String> = _filterCategory.asStateFlow()

    private val _filterMerchant = MutableStateFlow("")
    val filterMerchant: StateFlow<String> = _filterMerchant.asStateFlow()

    private val _filterOnlyWithReceipt = MutableStateFlow(false)
    val filterOnlyWithReceipt: StateFlow<Boolean> = _filterOnlyWithReceipt.asStateFlow()

    private val _filterOnlyUnclassified = MutableStateFlow(false)
    val filterOnlyUnclassified: StateFlow<Boolean> = _filterOnlyUnclassified.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadMockEntries() // TODO: 데모용 목 데이터, 추후 loadEntries()로 복원
    }

    private fun loadMockEntries() {
        _entries.value = listOf(
            BookEntryResponse(
                id = 1, entryDate = "2025-03-22", merchantName = "스타벅스 강남점",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 5500,
                fixedAssetAmount = 0, vatAmount = 500, supplyPrice = 5000,
                categoryCode = "welfare", categoryName = "복리후생비",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = true, note = "팀 미팅 커피", createdAt = "2025-03-22T14:23:00"
            ),
            BookEntryResponse(
                id = 2, entryDate = "2025-03-21", merchantName = "카카오T 택시",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 18400,
                fixedAssetAmount = 0, vatAmount = 1673, supplyPrice = 16727,
                categoryCode = "transport", categoryName = "여비교통비",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = true, createdAt = "2025-03-21T22:31:00"
            ),
            BookEntryResponse(
                id = 3, entryDate = "2025-03-20", merchantName = "클라이언트A",
                description = "3월 용역 대금",
                entryType = "INCOME", incomeAmount = 3300000, expenseAmount = 0,
                fixedAssetAmount = 0, vatAmount = 300000, supplyPrice = 3000000,
                categoryCode = "sales", categoryName = "매출",
                isBusinessExpense = false, isVatDeductible = false,
                confirmed = true, createdAt = "2025-03-20T10:00:00"
            ),
            BookEntryResponse(
                id = 4, entryDate = "2025-03-19", merchantName = "GS25 역삼점",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 3200,
                fixedAssetAmount = 0, vatAmount = 291, supplyPrice = 2909,
                categoryCode = "supplies", categoryName = "소모품비",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = false, createdAt = "2025-03-19T12:05:00"
            ),
            BookEntryResponse(
                id = 5, entryDate = "2025-03-18", merchantName = "교보문고",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 32000,
                fixedAssetAmount = 0, vatAmount = 2909, supplyPrice = 29091,
                categoryCode = "books", categoryName = "도서인쇄비",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = true, note = "개발 서적 구매", createdAt = "2025-03-18T15:40:00"
            ),
            BookEntryResponse(
                id = 6, entryDate = "2025-03-17", merchantName = "클라이언트B",
                description = "디자인 작업비",
                entryType = "INCOME", incomeAmount = 1100000, expenseAmount = 0,
                fixedAssetAmount = 0, vatAmount = 100000, supplyPrice = 1000000,
                categoryCode = "sales", categoryName = "매출",
                isBusinessExpense = false, isVatDeductible = false,
                confirmed = true, createdAt = "2025-03-17T09:00:00"
            ),
            BookEntryResponse(
                id = 7, entryDate = "2025-03-15", merchantName = "거래처 식사",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 85000,
                fixedAssetAmount = 0, vatAmount = 7727, supplyPrice = 77273,
                categoryCode = "entertain", categoryName = "접대비",
                isBusinessExpense = true, isVatDeductible = false,
                confirmed = true, note = "거래처 미팅 식사", createdAt = "2025-03-15T19:30:00"
            ),
            BookEntryResponse(
                id = 8, entryDate = "2025-03-14", merchantName = "애플코리아",
                entryType = "ASSET", incomeAmount = 0, expenseAmount = 0,
                fixedAssetAmount = 1990000, vatAmount = 180909, supplyPrice = 1809091,
                categoryCode = "asset", categoryName = "비품",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = true, note = "업무용 맥북", createdAt = "2025-03-14T11:00:00"
            ),
            BookEntryResponse(
                id = 9, entryDate = "2025-03-12", merchantName = "KT",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 55000,
                fixedAssetAmount = 0, vatAmount = 5000, supplyPrice = 50000,
                categoryCode = "comm", categoryName = "통신비",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = false, createdAt = "2025-03-12T00:00:00"
            ),
            BookEntryResponse(
                id = 10, entryDate = "2025-03-10", merchantName = "네이버클라우드",
                entryType = "EXPENSE", incomeAmount = 0, expenseAmount = 110000,
                fixedAssetAmount = 0, vatAmount = 10000, supplyPrice = 100000,
                categoryCode = "fee", categoryName = "지급수수료",
                isBusinessExpense = true, isVatDeductible = true,
                confirmed = true, note = "서버 호스팅비", createdAt = "2025-03-10T00:00:00"
            )
        )
    }

    fun loadEntries() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = bookEntryRepository.getEntries(confirmed = null, page = 0, size = 50)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _entries.value = response.body()?.data?.content ?: emptyList()
                } else {
                    _error.value = response.body()?.message ?: "데이터를 불러올 수 없습니다"
                }
            } catch (e: Exception) {
                _error.value = "네트워크 오류가 발생했습니다"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUnconfirmedCount() {
        viewModelScope.launch {
            try {
                val response = bookEntryRepository.getUnconfirmedCount()
                if (response.isSuccessful && response.body()?.status == "success") {
                    _unconfirmedCount.value = response.body()?.data ?: 0
                }
            } catch (_: Exception) { }
        }
    }

    fun selectYear(year: Int) {
        _selectedYear.value = year
    }

    fun selectMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun selectFilter(filter: EntryFilter) {
        _selectedFilter.value = filter
    }

    fun selectTab(tab: BookTab) {
        _selectedTab.value = tab
    }

    fun applyFilter(category: String, merchant: String, onlyReceipt: Boolean, onlyUnclassified: Boolean) {
        _filterCategory.value = category
        _filterMerchant.value = merchant
        _filterOnlyWithReceipt.value = onlyReceipt
        _filterOnlyUnclassified.value = onlyUnclassified
    }

    fun updateEntryCategory(entryId: Long, newCategory: String) {
        _entries.value = _entries.value.map { entry ->
            if (entry.id == entryId) entry.copy(categoryName = newCategory, confirmed = true)
            else entry
        }
    }

    fun resetFilter() {
        _filterCategory.value = "전체"
        _filterMerchant.value = ""
        _filterOnlyWithReceipt.value = false
        _filterOnlyUnclassified.value = false
    }

    private fun getEntriesForMonth(): List<BookEntryResponse> {
        val year = _selectedYear.value
        val month = _selectedMonth.value
        return _entries.value.filter {
            try {
                val date = java.time.LocalDate.parse(it.entryDate)
                date.year == year && date.monthValue == month
            } catch (_: Exception) { false }
        }
    }

    fun getFilteredEntries(): List<BookEntryResponse> {
        var result = getEntriesForMonth()

        // 탭 필터 (전체/수입/비용/자산)
        result = when (_selectedFilter.value) {
            EntryFilter.ALL -> result
            EntryFilter.INCOME -> result.filter { it.entryType == "INCOME" }
            EntryFilter.EXPENSE -> result.filter { it.entryType == "EXPENSE" }
            EntryFilter.ASSET -> result.filter { it.entryType == "ASSET" }
        }

        // 계정과목 필터
        val cat = _filterCategory.value
        if (cat != "전체") {
            result = result.filter { it.categoryName == cat }
        }

        // 거래처 검색
        val merchant = _filterMerchant.value
        if (merchant.isNotBlank()) {
            result = result.filter {
                (it.merchantName ?: "").contains(merchant, ignoreCase = true) ||
                (it.description ?: "").contains(merchant, ignoreCase = true)
            }
        }

        // 미분류 거래만
        if (_filterOnlyUnclassified.value) {
            result = result.filter { !it.confirmed }
        }

        return result
    }

    fun getTotalIncome(): Long = getEntriesForMonth().sumOf { it.incomeAmount }
    fun getTotalExpense(): Long = getEntriesForMonth().sumOf { it.expenseAmount }
    fun getTotalAsset(): Long = getEntriesForMonth().sumOf { it.fixedAssetAmount }
    fun getNetProfit(): Long = getTotalIncome() - getTotalExpense()

    // 세목별 비용 집계
    fun getExpenseByCategory(): List<Pair<String, Long>> {
        return getEntriesForMonth()
            .filter { it.entryType == "EXPENSE" && it.categoryName != null }
            .groupBy { it.categoryName!! }
            .map { (name, entries) -> name to entries.sumOf { it.expenseAmount } }
            .sortedByDescending { it.second }
    }

    // 세목별 수입 집계
    fun getIncomeByCategory(): List<Pair<String, Long>> {
        return getEntriesForMonth()
            .filter { it.entryType == "INCOME" && it.categoryName != null }
            .groupBy { it.categoryName!! }
            .map { (name, entries) -> name to entries.sumOf { it.incomeAmount } }
            .sortedByDescending { it.second }
    }
}
