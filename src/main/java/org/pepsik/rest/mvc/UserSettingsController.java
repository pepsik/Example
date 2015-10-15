package org.pepsik.rest.mvc;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.pepsik.core.models.entities.Profile;
import org.pepsik.core.models.entities.User;
import org.pepsik.core.models.entities.Password;
import org.pepsik.core.models.entities.support.EmailForm;
import org.pepsik.core.models.entities.support.MutableUserDetails;
import org.pepsik.core.services.SmartService;
import org.pepsik.core.models.entities.support.PasswordForm;
import org.pepsik.rest.exceptions.InsufficientAuthorizationException;
import org.pepsik.rest.utilities.support.HttpSessionCollector;
import org.pepsik.rest.utilities.support.PojoSessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.*;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for
 */

//@Controller
//@RequestMapping("/settings")
//TODO: Session attr lifecycle
public class UserSettingsController {

    public static final String DATE_PATTERN = "yyyy-MM-dd";

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsController.class);

    @Autowired
    private SmartService service;

    @Autowired
    private SessionRegistry sessionRegistry;

    @InitBinder
    public void initBinder(WebDataBinder binder) {

        DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_PATTERN);

        binder.registerCustomEditor(DateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                try {
                    setValue(fmt.parseDateTime(text));
                } catch (IllegalArgumentException ex) {
                    binder.getBindingResult().rejectValue("birthdate", "birthdate.emptyOrInvalid", "Empty or invalid");
                }
            }

            @Override
            public String getAsText() {
                return fmt.print((DateTime) getValue());
            }
        });

        binder.registerCustomEditor(String.class, "old_password", new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                setValue(text);
            }

            @Override
            public String getAsText() {
                return "";
            }
        });
    }

    @RequestMapping(value = "/profile", method = RequestMethod.GET, produces = "text/html")
    public String getProfile(Model model, HttpSession session) {
        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Profile profile = service.getProfile(loggedUser);
        session.setAttribute("profile", profile);
        model.addAttribute(profile);
        return "settings/profile";
    }

    @RequestMapping(value = "/profile", method = RequestMethod.PUT, produces = "text/html")
    public String updateProfile(@Valid Profile updatedProfile, BindingResult result, HttpSession session) {
        if (result.hasErrors())
            return "settings/profile";

        Profile oldProfile = (Profile) session.getAttribute("profile");
        session.removeAttribute("profile");

        updatedProfile.setId(oldProfile.getId());
        updatedProfile.setUser(oldProfile.getUser());
        updatedProfile.setEmail(oldProfile.getEmail());
        service.saveProfile(updatedProfile);
        return "redirect:/settings/profile";
    }

    @RequestMapping(value = "/account", method = RequestMethod.GET, produces = "text/html")
    public String getAccount(Model model, HttpSession session) {
        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = service.getUser(loggedUser);
        session.setAttribute("user", user);
        model.addAttribute(user);
        model.addAttribute(new PasswordForm());
        return "settings/account";
    }

    @RequestMapping(value = "/account/password", method = RequestMethod.PUT, produces = "text/html")
    public String updateAccountPassword(@Valid PasswordForm password, BindingResult result, HttpSession session, Model model) {

        final User user = (User) session.getAttribute("user");

        //TODO: try to hide validations in PasswordForm class
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password.getOld_password(), user.getUserPassword().getPassword()))
            result.addError(new FieldError("passwordForm", "old_password", "Old password isn't valid"));

        if (!password.getNew_password().equals(password.getRepeat_new_password()))
            result.addError(new FieldError("passwordForm", "repeat_new_password", "Confirm password not match"));

        if (result.hasErrors()) {
            model.addAttribute(user);
            return "settings/account";
        }

        Password userPassword = user.getUserPassword();
        userPassword.setPassword(encoder.encode(password.getNew_password()));
        user.setUserPassword(userPassword);
        service.saveUser(user);
        session.removeAttribute("user");
        return "redirect:/settings/account";
    }

    @RequestMapping(value = "/account/username", method = RequestMethod.PUT, produces = "text/html")
    public String updateAccountUsername(@Valid User updatedUser, BindingResult result, HttpSession session, Model model, HttpServletRequest request) {
        if (result.hasErrors()) {
            model.addAttribute(new PasswordForm());
            return "settings/account";
        }

        final User user = (User) session.getAttribute("user");
        updatedUser.setId(user.getId());
        service.saveUser(updatedUser);
        session.removeAttribute("user");

        //changes username of authenticated user
        MutableUserDetails mutableUserDetails = new MutableUserDetails((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        mutableUserDetails.setUsername(updatedUser.getUsername());
        Authentication authentication = new UsernamePasswordAuthenticationToken(mutableUserDetails, mutableUserDetails.getPassword(), mutableUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return "redirect:/settings/account";
    }

    @RequestMapping(value = "/emails", method = RequestMethod.GET, produces = "text/html")
    public String getEmails(Model model) {
        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Profile profile = service.getProfile(loggedUser);
        EmailForm email = new EmailForm();

        if (profile.getEmail() == null)
            email.setEmail("");
        else
            email.setEmail(profile.getEmail());
        model.addAttribute(email);
        return "settings/emails";
    }

    @RequestMapping(value = "/emails", method = RequestMethod.PUT, produces = "text/html")
    public String updateEmails(@Valid EmailForm emailForm, BindingResult result) {
        if (result.hasErrors())
            return "settings/emails";

        String loggedUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Profile profile = service.getProfile(loggedUser);
        profile.setEmail(emailForm.getEmail());
        service.saveProfile(profile);
        return "redirect:/settings/emails";
    }

    @RequestMapping(value = "/security", method = RequestMethod.GET, produces = "text/html")
    public String getSecurity(HttpServletRequest request, Model model) {
        List<PojoSessionInfo> pojoSessionsInfo = new ArrayList<>();
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        HttpSession httpSession;
        int count = 1;
        Map<String, String> sessionAliases = new HashMap<>();
        for (SessionInformation si : sessionRegistry.getAllSessions(principal, false)) {
            httpSession = HttpSessionCollector.find(si.getSessionId());
            PojoSessionInfo pojoSessionInfo = new PojoSessionInfo();
            pojoSessionInfo.setCreationDate(new DateTime(httpSession.getCreationTime()));
            pojoSessionInfo.setLastAccessedTime(new DateTime(httpSession.getLastAccessedTime()));
            pojoSessionInfo.setSessionId(httpSession.getId());
            pojoSessionInfo.setUserAgent((String) httpSession.getAttribute("User-Agent"));
            pojoSessionInfo.setUserRemoteIp((String) httpSession.getAttribute("RemoteAddress"));
            pojoSessionsInfo.add(pojoSessionInfo);
            sessionAliases.put("" + count++, httpSession.getId());
        }

        httpSession = request.getSession();
        httpSession.setAttribute("sessionAliases", sessionAliases);
        model.addAttribute("sessionsInfo", pojoSessionsInfo);
        return "settings/security";
    }

    @RequestMapping(value = "/security/session/{id}/revoke", method = RequestMethod.POST, produces = "text/html")
    public String revokeSession(HttpServletRequest request, @PathVariable("id") String sessionAlias) {
        HttpSession httpSession = request.getSession();
        Map<String, String> sessionAliasesMap = (Map<String, String>) httpSession.getAttribute("sessionAliases");
        if (sessionAliasesMap.containsKey(sessionAlias)) {
            SessionInformation sessionInformation = sessionRegistry.getSessionInformation(sessionAliasesMap.get(sessionAlias));
            sessionInformation.expireNow();
            sessionAliasesMap.remove(sessionAlias);
            service.removeUserRememberMeTokens(SecurityContextHolder.getContext().getAuthentication().getName());
        } else
            logger.error("Can't revoke session because session doesn't exist with given alias - " + sessionAlias);
        return "redirect:/settings/security";
    }

    @ExceptionHandler(InsufficientAuthorizationException.class)
    public ModelAndView insufficientAuthorizationError(HttpServletRequest req) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("login");
        mav.addObject("targetUrl", req.getRequestURI());
        return mav;
    }
}

