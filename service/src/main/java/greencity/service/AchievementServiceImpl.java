package greencity.service;

import greencity.constant.CacheConstants;
import greencity.constant.ErrorMessage;
import greencity.dto.PageableAdvancedDto;
import greencity.dto.achievement.*;
import greencity.dto.achievementcategory.AchievementCategoryVO;
import greencity.dto.user.UserVO;
import greencity.entity.Achievement;
import greencity.entity.AchievementCategory;
import greencity.exception.exceptions.NotDeletedException;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.NotUpdatedException;
import greencity.repository.AchievementRepo;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@EnableCaching
public class AchievementServiceImpl implements AchievementService {
    private final AchievementRepo achievementRepo;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final AchievementCategoryService achievementCategoryService;

    /**
     * {@inheritDoc}
     *
     * @author Orest Mamchuk
     */
    @Override
    public AchievementVO save(AchievementPostDto achievementPostDto) {
        Achievement achievement = modelMapper.map(achievementPostDto, Achievement.class);
        AchievementCategoryVO achievementCategoryVO =
            achievementCategoryService.findByName(achievementPostDto.getAchievementCategory().getName());
        achievement.getTranslations().forEach(adviceTranslation -> adviceTranslation.setAchievement(achievement));
        achievement.setAchievementCategory(modelMapper.map(achievementCategoryVO, AchievementCategory.class));
        AchievementVO achievementVO = modelMapper.map(achievementRepo.save(achievement), AchievementVO.class);
        UserAchievementVO userAchievementVO = new UserAchievementVO();
        userAchievementVO.setAchievement(achievementVO);
        List<UserVO> all = userService.findAll();
        all.forEach(userVO -> {
            userVO.getUserAchievements().add(userAchievementVO);
            userAchievementVO.setUser(userVO);
            userService.save(userVO);
        });
        return achievementVO;
    }

    /**
     * {@inheritDoc}
     *
     * @author Yuriy Olkhovskyi
     */
    @Cacheable(value = CacheConstants.ALL_ACHIEVEMENTS_CACHE_NAME)
    @Override
    public List<AchievementDTO> findAll() {
        return achievementRepo.findAll()
            .stream()
            .map(achieve -> modelMapper.map(achieve, AchievementDTO.class))
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @author Orest Mamchuk
     */
    @Override
    public PageableAdvancedDto<AchievementVO> findAll(Pageable page) {
        Page<Achievement> pages = achievementRepo.findAll(page);
        List<AchievementVO> achievementVOS = pages
            .stream()
            .map(achievement -> modelMapper.map(achievement, AchievementVO.class))
            .collect(Collectors.toList());
        return createPageable(achievementVOS, pages);
    }

    private PageableAdvancedDto<AchievementVO> createPageable(List<AchievementVO> achievementVOS,
        Page<Achievement> pages) {
        return new PageableAdvancedDto<>(
            achievementVOS,
            pages.getTotalElements(),
            pages.getPageable().getPageNumber(),
            pages.getTotalPages(),
            pages.getNumber(),
            pages.hasPrevious(),
            pages.hasNext(),
            pages.isFirst(),
            pages.isLast());
    }

    /**
     * {@inheritDoc}
     *
     * @author Orest Mamchuk
     */
    @Override
    public PageableAdvancedDto<AchievementVO> searchAchievementBy(Pageable paging, String query) {
        Page<Achievement> page = achievementRepo.searchAchievementsBy(paging, query);

        List<AchievementVO> achievementVOS = page.stream()
            .map(achievement -> modelMapper.map(achievement, AchievementVO.class))
            .collect(Collectors.toList());
        return createPageable(achievementVOS, page);
    }

    /**
     * {@inheritDoc}
     *
     * @author Orest Mamchuk
     */
    @Override
    public Long delete(Long id) {
        try {
            achievementRepo.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotDeletedException(ErrorMessage.ACHIEVEMENT_NOT_DELETED);
        }
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * @author Orest Mamchuk
     */
    @Override
    public void deleteAll(List<Long> listId) {
        listId.forEach(achievementRepo::deleteById);
    }

    /**
     * {@inheritDoc}
     *
     * @author Orest Mamchuk
     */
    @Override
    public AchievementVO findById(Long id) {
        return modelMapper.map(
            achievementRepo.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.ACHIEVEMENT_NOT_FOUND_BY_ID + id)),
            AchievementVO.class);
    }

    /**
     * {@inheritDoc}
     *
     * @author Orest Mamchuk
     */
    @Override
    public AchievementPostDto update(AchievementManagementDto achievementManagementDto) {
        Achievement achievement = achievementRepo.findById(achievementManagementDto.getId())
            .orElseThrow(() -> new NotUpdatedException(ErrorMessage.ACHIEVEMENT_NOT_FOUND_BY_ID
                + achievementManagementDto.getId()));
        setTranslations(achievement, achievementManagementDto);
        achievement.setCondition(achievementManagementDto.getCondition());
        Achievement updated = achievementRepo.save(achievement);
        return modelMapper.map(updated, AchievementPostDto.class);
    }

    private void setTranslations(Achievement achievement, AchievementManagementDto achievementManagementDto) {
        achievement.getTranslations()
            .forEach(achievementTranslation -> {
                AchievementTranslationVO achievementTranslationVO = achievementManagementDto
                    .getTranslations().stream()
                    .filter(newTranslation -> newTranslation.getLanguage().getCode()
                        .equals(achievementTranslation.getLanguage().getCode()))
                    .findFirst().get();
                achievementTranslation.setTitle(achievementTranslationVO.getTitle());
                achievementTranslation.setDescription(achievementTranslationVO.getDescription());
                achievementTranslation.setMessage(achievementTranslationVO.getMessage());
            });
    }
}