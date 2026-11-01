package expensemanagement.service;

import expensemanagement.dao.CategoryDAO;
import expensemanagement.exception.ValidationException;
import expensemanagement.model.Category;
import expensemanagement.model.User;
import expensemanagement.util.InputHelper;

import java.util.List;

/** Validates and manages categories owned by a user. */
public class CategoryService {
    private final CategoryDAO categoryDAO;

    /** Creates the service using its persistence dependency. */
    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    /** Returns the user's categories. */
    public List<Category> findAll(User user) {
        return categoryDAO.findAll(user.id());
    }

    /** Creates a category owned by the current user. */
    public Category add(User user, String name, String color) {
        return categoryDAO.create(user.id(), InputHelper.requiredText(name, "Category name", 80),
                InputHelper.color(color));
    }

    /** Renames and recolors one of the user's categories. */
    public void update(User user, long categoryId, String name, String color) {
        categoryDAO.update(user.id(), categoryId, InputHelper.requiredText(name, "Category name", 80),
                InputHelper.color(color));
    }

    /** Deletes one of the user's categories and its dependent data. */
    public void delete(User user, long categoryId) {
        if (categoryDAO.findAll(user.id()).stream().noneMatch(category -> category.id() == categoryId)) {
            throw new ValidationException("The selected category is no longer available.");
        }
        categoryDAO.delete(user.id(), categoryId);
    }
}
