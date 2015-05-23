package org.pepsik.validator;

import org.pepsik.model.User;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Created by pepsik on 4/18/15.
 */
public class AccountValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors e) {
        ValidationUtils.rejectIfEmptyOrWhitespace(e, "username", "username.emptyOrWhitespace");
        ValidationUtils.rejectIfEmptyOrWhitespace(e, "password", "password.emptyOrWhitespace");

        User user = (User) target;
        if (user.getUserPassword().getPassword().length() < 3)
            e.rejectValue("password", "password.short");

        if (user.getUserPassword().getPassword().length() > 20)
            e.rejectValue("password", "password.tooLarge");

        if (user.getUsername().length() < 3)
            e.rejectValue("username", "username.short");

        if (user.getUsername().length() > 20)
            e.rejectValue("username", "username.tooLarge");

    }
}
