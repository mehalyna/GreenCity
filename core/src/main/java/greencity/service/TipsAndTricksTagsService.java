package greencity.service;

import greencity.entity.TipsAndTricksTag;
import java.util.List;

/**
 * Provides the interface to manage {@link TipsAndTricksTag} entity.
 */
public interface TipsAndTricksTagsService {
    /**
     * Method that allow you to find all {@link TipsAndTricksTag}.
     *
     * @return list of {@link TipsAndTricksTag}'s names
     */
    List<String> findAll();

    /**
     * Method that allow you to find {@link TipsAndTricksTag} by name.
     *
     * @param name a value of {@link String}
     * @return {@link TipsAndTricksTag}
     */
    TipsAndTricksTag findByName(String name);

    /**
     * Method that allow you to find list of {@link TipsAndTricksTag} by names.
     *
     * @param tipsAndTricksTagsNames list of {@link String} values
     * @return list of {@link TipsAndTricksTag}
     */
    List<TipsAndTricksTag> findAllByNames(List<String> tipsAndTricksTagsNames);

    /**
     * Method that checks if all {@link TipsAndTricksTag}s are unique.
     *
     * @param tipsAndTricksTagsNames list of {@link String} values
     * @return {@link Boolean}
     */
    Boolean isAllValid(List<String> tipsAndTricksTagsNames);

    /**
     * Method that checks if there is allowed amount (less than 3) of unique {@link TipsAndTricksTag}s .
     *
     * @param tipsAndTricksTagsNames list of {@link String} values
     * @return {@link Boolean}
     */
    Boolean isValidNumOfUniqueTags(List<String> tipsAndTricksTagsNames);
}