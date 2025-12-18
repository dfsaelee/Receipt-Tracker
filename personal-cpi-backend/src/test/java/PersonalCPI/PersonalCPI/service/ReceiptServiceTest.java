package PersonalCPI.PersonalCPI.service;

import PersonalCPI.PersonalCPI.dto.MonthlySpendingDto;
import PersonalCPI.PersonalCPI.dto.ReceiptCreateDto;
import PersonalCPI.PersonalCPI.dto.ReceiptResponseDto;
import PersonalCPI.PersonalCPI.dto.SpendingSummaryDto;
import PersonalCPI.PersonalCPI.model.Category;
import PersonalCPI.PersonalCPI.model.Receipt;
import PersonalCPI.PersonalCPI.repository.CategoryRepository;
import PersonalCPI.PersonalCPI.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ReceiptService receiptService;

    private Category groceries;
    private Category dining;

    @BeforeEach
    void setUp() {
        groceries = buildCategory(10L, "Groceries");
        dining = buildCategory(20L, "Dining Out");
    }

    @Test
    void createReceipt_withValidCategory_persistsReceiptAndReturnsDto() {
        ReceiptCreateDto dto = new ReceiptCreateDto();
        dto.setStoreName("Trader Joe's");
        dto.setPurchaseDate(LocalDate.of(2025, 1, 5));
        dto.setCategoryId(groceries.getCategoryId());
        dto.setAmount(new BigDecimal("42.37"));

        when(categoryRepository.findById(groceries.getCategoryId())).thenReturn(Optional.of(groceries));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> {
            Receipt receipt = invocation.getArgument(0);
            ReflectionTestUtils.setField(receipt, "receiptId", 99L);
            return receipt;
        });

        ReceiptResponseDto response = receiptService.createReceipt(1L, dto);

        ArgumentCaptor<Receipt> receiptArgumentCaptor = ArgumentCaptor.forClass(Receipt.class);
        verify(receiptRepository).save(receiptArgumentCaptor.capture());
        Receipt savedReceipt = receiptArgumentCaptor.getValue();

        assertThat(savedReceipt.getUserId()).isEqualTo(1L);
        assertThat(savedReceipt.getAmount()).isEqualByComparingTo("42.37");
        assertThat(response.getReceiptId()).isEqualTo(99L);
        assertThat(response.getCategoryName()).isEqualTo("Groceries");
    }

    @Test
    void createReceipt_withUnknownCategory_throwsIllegalArgument() {
        ReceiptCreateDto dto = new ReceiptCreateDto();
        dto.setCategoryId(123L);
        dto.setAmount(BigDecimal.TEN);

        when(categoryRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receiptService.createReceipt(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");

        verify(receiptRepository, never()).save(any());
    }

    @Test
    void getSpendingByCategoryForPeriod_returnsSummariesWithNames() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        when(receiptRepository.getSpendingSummaryByCategory(1L, start, end))
                .thenReturn(List.of(
                        new Object[]{groceries.getCategoryId(), new BigDecimal("120.00")},
                        new Object[]{dining.getCategoryId(), new BigDecimal("45.50")}
                ));
        when(categoryRepository.findAll()).thenReturn(List.of(groceries, dining));

        List<SpendingSummaryDto> summaries = receiptService.getSpendingByCategoryForPeriod(1L, start, end);

        assertThat(summaries)
                .extracting(SpendingSummaryDto::getCategoryName)
                .containsExactlyInAnyOrder("Groceries", "Dining Out");
        assertThat(summaries)
                .extracting(SpendingSummaryDto::getTotalAmount)
                .containsExactlyInAnyOrder(new BigDecimal("120.00"), new BigDecimal("45.50"));
    }

    @Test
    void getMonthlySpendingSummary_returnsDescendingMonths() {
        when(receiptRepository.getMonthlySpendingSummary(1L))
                .thenReturn(List.of(
                        new Object[]{2025, 2, new BigDecimal("300.00")},
                        new Object[]{2025, 1, new BigDecimal("150.00")}
                ));

        List<MonthlySpendingDto> summaries = receiptService.getMonthlySpendingSummary(1L);

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).getYear()).isEqualTo(2025);
        assertThat(summaries.get(0).getMonth()).isEqualTo(2);
        assertThat(summaries.get(0).getTotalAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    void deleteReceipt_withMismatchedUser_throwsIllegalArgument() {
        Receipt receipt = new Receipt();
        receipt.setAmount(BigDecimal.ONE);
        receipt.setUserId(2L);
        when(receiptRepository.findById(5L)).thenReturn(Optional.of(receipt));

        assertThatThrownBy(() -> receiptService.deleteReceipt(1L, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to user");
    }

    @Test
    void getCurrentMonthSpending_delegatesToSummaryMethod() {
        YearMonth current = YearMonth.now();
        LocalDate start = current.atDay(1);
        LocalDate end = current.atEndOfMonth();
        List<Object[]> repoResults = Collections.singletonList(
                new Object[]{groceries.getCategoryId(), BigDecimal.TEN}
        );
        when(receiptRepository.getSpendingSummaryByCategory(anyLong(), eq(start), eq(end)))
                .thenReturn(repoResults);
        when(categoryRepository.findAll()).thenReturn(List.of(groceries));

        List<SpendingSummaryDto> summaries = receiptService.getCurrentMonthSpendingByCategory(7L);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getCategoryName()).isEqualTo("Groceries");
        verify(receiptRepository).getSpendingSummaryByCategory(7L, start, end);
    }

    private Category buildCategory(Long id, String name) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setName(name);
        return category;
    }
}

