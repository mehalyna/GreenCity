package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.dto.PageableDto;
import greencity.dto.habit.HabitVO;
import greencity.dto.habitfact.*;
import greencity.dto.language.LanguageTranslationDTO;
import greencity.entity.Habit;
import greencity.entity.HabitFact;
import greencity.entity.HabitFactTranslation;
import greencity.entity.Language;
import greencity.exception.exceptions.NotDeletedException;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.NotUpdatedException;
import greencity.exception.exceptions.WrongIdException;
import greencity.filters.HabitFactSpecification;
import greencity.filters.SearchCriteria;
import greencity.repository.HabitFactRepo;
import greencity.repository.HabitFactTranslationRepo;
import greencity.repository.HabitRepo;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link HabitFactService}.
 *
 * @author Vitaliy Dzen
 */
@Service
@AllArgsConstructor
public class HabitFactServiceImpl implements HabitFactService {
    private final HabitFactRepo habitFactRepo;
    private final HabitFactTranslationRepo habitFactTranslationRepo;
    private final ModelMapper modelMapper;
    private final HabitRepo habitRepo;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageableDto<LanguageTranslationDTO> getAllHabitFacts(Pageable page, String language) {
        Page<HabitFactTranslation> habitFactTranslation = habitFactTranslationRepo
            .findAllByLanguageCode(page, language);
        return getPagesWithLanguageTranslationDTO(habitFactTranslation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageableDto<HabitFactVO> getAllHabitFactsVO(Pageable pageable) {
        Page<HabitFact> page = habitFactRepo.findAll(pageable);
        List<HabitFactVO> habitFactVOS = page.stream()
            .map(habitFact -> modelMapper.map(habitFact, HabitFactVO.class))
            .collect(Collectors.toList());
        return new PageableDto<>(
            habitFactVOS,
            page.getTotalElements(),
            page.getPageable().getPageNumber(),
            page.getTotalPages());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LanguageTranslationDTO getRandomHabitFactByHabitIdAndLanguage(Long id, String language) {
        return modelMapper.map(habitFactTranslationRepo.getRandomHabitFactTranslationByHabitIdAndLanguage(language, id)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.HABIT_FACT_NOT_FOUND_BY_ID + id)),
            LanguageTranslationDTO.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HabitFactDtoResponse getHabitFactById(Long id) {
        HabitFactVO habitFactVO = modelMapper.map(habitFactRepo.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.HABIT_FACT_NOT_FOUND_BY_ID + id)),
            HabitFactVO.class);
        return modelMapper.map(habitFactVO, HabitFactDtoResponse.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HabitFactDto getHabitFactByName(String language, String name) {
        return modelMapper.map(habitFactTranslationRepo.findFactTranslationByLanguageCodeAndContent(language, name)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.HABIT_FACT_NOT_FOUND_BY_ID + name)),
            HabitFactDto.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HabitFactVO save(HabitFactPostDto fact) {
        HabitFact map = modelMapper.map(fact, HabitFact.class);
        return modelMapper.map(habitFactRepo.save(map), HabitFactVO.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HabitFactVO update(HabitFactPostDto factDto, Long id) {
        HabitFact habitFactFromDB = habitFactRepo.findById(id)
            .map(habitFact -> {
                Habit habit = habitRepo.findById(factDto.getHabit().getId())
                    .orElseThrow(() -> new WrongIdException(ErrorMessage.HABIT_NOT_FOUND_BY_ID));
                habitFact.setHabit(habit);
                updateHabitTranslations(habitFact.getTranslations(), factDto.getTranslations());
                return habitFactRepo.save(habitFact);
            }).orElseThrow(() -> new NotUpdatedException(ErrorMessage.HABIT_FACT_NOT_UPDATED_BY_ID));
        return modelMapper.map(habitFactFromDB, HabitFactVO.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long delete(Long id) {
        if (habitFactRepo.findById(id).isEmpty()) {
            throw new NotDeletedException(ErrorMessage.HABIT_FACT_NOT_DELETED_BY_ID);
        }
        habitFactRepo.deleteById(id);
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteAllByHabit(HabitVO habit) {
        habitFactRepo.findAllByHabitId(habit.getId())
            .forEach(habitFact -> {
                habitFactTranslationRepo.deleteAllByHabitFact(habitFact);
                habitFactRepo.delete(habitFact);
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public List<Long> deleteAllHabitFactsByListOfId(List<Long> listId) {
        listId.forEach(id -> {
            HabitFact habitFact = habitFactRepo.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.HABIT_FACT_NOT_FOUND_BY_ID + id));
            habitFactTranslationRepo.deleteAllByHabitFact(habitFact);
            habitFactRepo.delete(habitFact);
        });
        return listId;
    }

    private PageableDto<LanguageTranslationDTO> getPagesWithLanguageTranslationDTO(Page<HabitFactTranslation> pages) {
        List<LanguageTranslationDTO> languageTranslationDTOS = pages
            .stream()
            .map(habitFactTranslation -> modelMapper.map(habitFactTranslation, LanguageTranslationDTO.class))
            .collect(Collectors.toList());
        return new PageableDto<>(
            languageTranslationDTOS,
            pages.getTotalElements(),
            pages.getPageable().getPageNumber(),
            pages.getTotalPages()
        );
    }

    private void updateHabitTranslations(List<HabitFactTranslation> habitFactTranslations,
                                         List<LanguageTranslationDTO> languageTranslationDTOS) {
        Iterator<HabitFactTranslation> adviceTranslationIterator = habitFactTranslations.iterator();
        Iterator<LanguageTranslationDTO> languageTranslationDTOIterator = languageTranslationDTOS.iterator();
        while (adviceTranslationIterator.hasNext() && languageTranslationDTOIterator.hasNext()) {
            HabitFactTranslation habitFactTranslation = adviceTranslationIterator.next();
            LanguageTranslationDTO languageDTO = languageTranslationDTOIterator.next();
            habitFactTranslation.setContent(languageDTO.getContent());
            habitFactTranslation.setLanguage(modelMapper.map(languageDTO.getLanguage(), Language.class));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageableDto<HabitFactVO> searchBy(Pageable paging, String query) {
        Page<HabitFact> page = habitFactRepo.searchBy(paging, query);
        List<HabitFactVO> habitFactVOS = page.stream()
            .map(habitFact -> modelMapper.map(habitFact, HabitFactVO.class))
            .collect(Collectors.toList());
        return new PageableDto<>(
            habitFactVOS,
            page.getTotalElements(),
            page.getPageable().getPageNumber(),
            page.getTotalPages());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageableDto<HabitFactVO> getFilteredDataForManagementByPage(
        Pageable pageable,
        HabitFactViewDto habitFactViewDto) {
        Page<HabitFact> pages = habitFactRepo.findAll(getSpecification(habitFactViewDto), pageable);
        return getPagesWithHabitFactVO(pages);
    }

    /**
     * * This method used for build {@link SearchCriteria} depends on {@link HabitFactViewDto}.
     *
     * @param habitFactViewDto used for receive parameters for filters from UI.
     * @return {@link SearchCriteria}.
     */
    private List<SearchCriteria> buildSearchCriteria(HabitFactViewDto habitFactViewDto) {
        List<SearchCriteria> criteriaList = new ArrayList<>();
        SearchCriteria searchCriteria;
        if (!habitFactViewDto.getId().isEmpty()) {
            searchCriteria = SearchCriteria.builder()
                .key("id")
                .type("id")
                .value(habitFactViewDto.getId())
                .build();
            criteriaList.add(searchCriteria);
        }
        if (!habitFactViewDto.getHabitId().isEmpty()) {
            searchCriteria = SearchCriteria.builder()
                .key("habitId")
                .type("habitId")
                .value(habitFactViewDto.getHabitId())
                .build();
            criteriaList.add(searchCriteria);
        }
        if (!habitFactViewDto.getContent().isEmpty()) {
            searchCriteria = SearchCriteria.builder()
                .key("content")
                .type("content")
                .value(habitFactViewDto.getContent())
                .build();
            criteriaList.add(searchCriteria);
        }
        return criteriaList;
    }

    /**
     * Returns {@link HabitFactSpecification} for entered filter parameters.
     *
     * @param habitFactViewDto contains data from filters
     */
    private HabitFactSpecification getSpecification(HabitFactViewDto habitFactViewDto) {
        List<SearchCriteria> searchCriteria = buildSearchCriteria(habitFactViewDto);
        return new HabitFactSpecification(searchCriteria);
    }

    private PageableDto<HabitFactVO> getPagesWithHabitFactVO(Page<HabitFact> pages) {
        List<HabitFactVO> habitFactVOS = pages
            .stream()
            .map(habitFact -> modelMapper.map(habitFact, HabitFactVO.class))
            .collect(Collectors.toList());
        return new PageableDto<>(
            habitFactVOS,
            pages.getTotalElements(),
            pages.getPageable().getPageNumber(),
            pages.getTotalPages());
    }
}
