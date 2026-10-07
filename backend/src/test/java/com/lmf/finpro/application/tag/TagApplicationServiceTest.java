package com.lmf.finpro.application.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.InvalidTagException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.TagAlreadyExistsException;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.port.out.TagRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagApplicationServiceTest {

    private static final Long USER_ID = 10L;

    @Mock private TagRepositoryPort tagRepositoryPort;

    @InjectMocks private TagApplicationService service;

    private static Tag tag(long id, String name) {
        return new Tag(id, USER_ID, name, null, LocalDateTime.now());
    }

    @Test
    void resolveReusesExistingTagsCreatesTheRestAndIgnoresRepeatedSpellings() {
        when(tagRepositoryPort.findAllByHouseholdIdAndNames(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(tag(1, "site-acme")));
        AtomicLong nextId = new AtomicLong(100);
        when(tagRepositoryPort.save(any()))
                .thenAnswer(
                        invocation -> {
                            Tag created = invocation.getArgument(0);
                            return new Tag(
                                    nextId.getAndIncrement(),
                                    created.householdId(),
                                    created.name(),
                                    created.color(),
                                    created.createdAt());
                        });

        List<Tag> resolved =
                service.resolveOrCreate(
                        USER_ID, List.of("#Site Acme", "site-acme", "Dedutível", " #dedutível "));

        assertThat(resolved).extracting(Tag::name).containsExactly("site-acme", "dedutível");
        assertThat(resolved).extracting(Tag::id).containsExactly(1L, 100L);
        verify(tagRepositoryPort, times(1)).save(any());
    }

    @Test
    void refusesMoreThanTheTagLimitPerItem() {
        List<String> tooMany =
                IntStream.rangeClosed(0, Tag.MAX_TAGS_PER_ITEM).mapToObj(i -> "t" + i).toList();

        assertThatThrownBy(() -> service.resolveOrCreate(USER_ID, tooMany))
                .isInstanceOf(InvalidTagException.class)
                .hasMessageContaining("no máximo");
        verify(tagRepositoryPort, never()).save(any());
    }

    @Test
    void noNamesMeansNoTags() {
        assertThat(service.resolveOrCreate(USER_ID, null)).isEmpty();
        assertThat(service.resolveOrCreate(USER_ID, List.of())).isEmpty();
    }

    @Test
    void replacingTagsLinksTheResolvedIds() {
        when(tagRepositoryPort.findAllByHouseholdIdAndNames(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(tag(1, "site-acme")));

        service.replaceTransactionTags(USER_ID, 55L, List.of("site-acme"));

        verify(tagRepositoryPort).replaceTransactionTags(55L, List.of(1L));
    }

    @Test
    void createAndRenameRefuseANameAlreadyInUse() {
        when(tagRepositoryPort.findAllByHouseholdIdAndNames(USER_ID, List.of("site-acme")))
                .thenReturn(List.of(tag(1, "site-acme")));
        when(tagRepositoryPort.findById(2L)).thenReturn(Optional.of(tag(2, "outra")));

        assertThatThrownBy(() -> service.create(USER_ID, "#Site Acme", null))
                .isInstanceOf(TagAlreadyExistsException.class)
                .hasMessage("Já existe a tag #site-acme.");
        assertThatThrownBy(() -> service.update(USER_ID, 2L, "site acme", null))
                .isInstanceOf(TagAlreadyExistsException.class);
    }

    @Test
    void renamingATagToItsOwnNameIsAllowed() {
        when(tagRepositoryPort.findById(1L)).thenReturn(Optional.of(tag(1, "site-acme")));
        when(tagRepositoryPort.findAllByHouseholdIdAndNames(USER_ID, List.of("site-acme")))
                .thenReturn(List.of(tag(1, "site-acme")));
        when(tagRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Tag updated = service.update(USER_ID, 1L, "Site Acme", "#2ad6a5");

        assertThat(updated.color()).isEqualTo("#2ad6a5");
    }

    @Test
    void tagOfAnotherUserIsNotFound() {
        when(tagRepositoryPort.findById(1L))
                .thenReturn(Optional.of(new Tag(1L, 99L, "x", null, LocalDateTime.now())));

        assertThatThrownBy(() -> service.delete(USER_ID, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(tagRepositoryPort, never()).deleteById(any());
    }

    @Test
    void listCarriesHowManyTransactionsUseEachTag() {
        when(tagRepositoryPort.findAllByHouseholdId(USER_ID))
                .thenReturn(List.of(tag(1, "a"), tag(2, "b")));
        when(tagRepositoryPort.countTransactionsByTagIds(List.of(1L, 2L)))
                .thenReturn(Map.of(1L, 3L));

        assertThat(service.list(USER_ID))
                .extracting(TagApplicationService.TagUsage::transactionCount)
                .containsExactly(3L, 0L);
    }
}
