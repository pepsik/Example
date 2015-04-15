package org.pepsik.web;

import org.joda.time.DateTime;
import org.pepsik.model.Account;
import org.pepsik.service.SmartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by pepsik on 4/9/15.
 */

@Controller
@RequestMapping("/account")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private SmartService service;

    @RequestMapping(value = "/new)", method = RequestMethod.GET)
    public String newAccount(Model model) {
        model.addAttribute("account", new Account());
        return "account/create";
    }

    @RequestMapping(value = "/{id}/edit", method = RequestMethod.GET)
    public String editAccount(@PathVariable("id") long id, HttpSession session, Model model) {
        Account account = service.getAccount(id);
        session.setAttribute("account", account);
        model.addAttribute("account", account);
        return "account/edit";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String createAccount(HttpServletRequest request, Model model) {
        Account account = new Account();
        account.setUsername((String) request.getParameter("username"));
        account.setFullname((String) request.getParameter("fullname"));
        account.setPassword((String) request.getParameter("password"));
        account.setBirthdate(DateTime.parse(request.getParameter("birthdate")));
//        account.setBirthdate(new DateTime());

//        Enumeration enumeration = request.getAttributeNames();
//        for (; enumeration.hasMoreElements(); )
//            logger.info(enumeration.nextElement().toString());
//
//        try {
//            BufferedReader bufferedReader = request.getReader();
//
//            String line;
//            while ((line = bufferedReader.readLine()) != null)
//                logger.info(line);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        service.saveAccount(account);       //TODO: Validation
        model.addAttribute("account", account);
        return "account/view";
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String getAccount(@PathVariable("id") long id, Model model) {
        model.addAttribute("account", service.getAccount(id));
        return "account/view";
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public String updateAccount(@PathVariable("id") long id, Account editedAccount, HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("account");
        editedAccount.setId(account.getId());
        model.addAttribute(editedAccount);
        return "account/view";
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public String deleteAccount(@PathVariable("id") long id) {
        service.deleteAccount(id);
        return "redirect:/home";
    }
}
