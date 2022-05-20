package ru.mirea.pcmatrix.contoller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import ru.mirea.pcmatrix.dao.ProductDAO;
import ru.mirea.pcmatrix.dao.OrderDAO;
import ru.mirea.pcmatrix.entity.Product;
import ru.mirea.pcmatrix.form.CustomerForm;
import ru.mirea.pcmatrix.model.StoreInfo;
import ru.mirea.pcmatrix.model.ProductInfo;
import ru.mirea.pcmatrix.model.CustomerInfo;
import ru.mirea.pcmatrix.pagination.PaginationResult;
import ru.mirea.pcmatrix.utils.Utils;
import ru.mirea.pcmatrix.validator.CustomerFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Transactional
public class MainController {

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private ProductDAO productDAO;

    @Autowired
    private CustomerFormValidator customerFormValidator;

    @InitBinder
    public void myInitBinder(WebDataBinder dataBinder) {
        Object target = dataBinder.getTarget();
        if (target == null) {
            return;
        }
        System.out.println("Target=" + target);

        // Case update quantity in cart
        //(@ModelAttribute("storeForm") @Validated StoreInfo storeForm)
        if (target.getClass() == StoreInfo.class) {

        }

        // Case save customer information.
        // (@ModelAttribute @Validated CustomerInfo customerForm)
        else if (target.getClass() == CustomerForm.class) {
            dataBinder.setValidator(customerFormValidator);
        }

    }


    @RequestMapping("/403")
    public String accessDenied() {
        return "/403";
    }

    @RequestMapping("/")
    public String home() {
        return "index";
    }

    // Product List
    @RequestMapping({ "/productList" })
    public String listProductHandler(Model model, //
                                     @RequestParam(value = "name", defaultValue = "") String likeName,
                                     @RequestParam(value = "page", defaultValue = "1") int page) {
        final int maxResult = 7;
        final int maxNavigationPage = 10;

        PaginationResult<ProductInfo> result = productDAO.queryProducts(page, //
                maxResult, maxNavigationPage, likeName);

        model.addAttribute("paginationProducts", result);
        return "productList";
    }

    @RequestMapping({ "/buyProduct" })
    public String listProductHandler(HttpServletRequest request, Model model, //
                                     @RequestParam(value = "code", defaultValue = "") String code) {

        Product product = null;
        if (code != null && code.length() > 0) {
            product = productDAO.findProduct(code);
        }
        if (product != null) {

            //
            StoreInfo storeInfo = Utils.getStoreInSession(request);

            ProductInfo productInfo = new ProductInfo(product);

            storeInfo.addProduct(productInfo, 1);
        }

        return "redirect:/pcmatrix";
    }

    @RequestMapping({ "/pcmatrixRemoveProduct" })
    public String removeProductHandler(HttpServletRequest request, Model model, //
                                       @RequestParam(value = "code", defaultValue = "") String code) {
        Product product = null;
        if (code != null && code.length() > 0) {
            product = productDAO.findProduct(code);
        }
        if (product != null) {

            StoreInfo storeInfo = Utils.getStoreInSession(request);

            ProductInfo productInfo = new ProductInfo(product);

            storeInfo.removeProduct(productInfo);

        }

        return "redirect:/pcmatrix";
    }

    // POST: Update quantity for product in store
    @RequestMapping(value = { "/pcmatrix" }, method = RequestMethod.POST)
    public String pcmatrixUpdateQty(HttpServletRequest request, //
                                        Model model, //
                                        @ModelAttribute("storeForm") StoreInfo storeForm) {

        StoreInfo storeInfo = Utils.getStoreInSession(request);
        storeInfo.updateQuantity(storeForm);

        return "redirect:/pcmatrix";
    }

    // GET: Show cart.

    @RequestMapping(value = { "/pcmatrix" }, method = RequestMethod.GET)
    public String pcmatrixHandler(HttpServletRequest request, Model model) {
        return doIt1(request, model);
    }

    private String doIt1(HttpServletRequest request, Model model){
        StoreInfo myStore = Utils.getStoreInSession(request);

        model.addAttribute("storeForm", myStore);
        return "pcmatrix";
    }

    // GET: Enter customer information.

    @RequestMapping(value = { "/pcmatrixCustomer" }, method = RequestMethod.GET)
    public String pcmatrixCustomerForm(HttpServletRequest request, Model model) {

        StoreInfo storeInfo = Utils.getStoreInSession(request);

        if (storeInfo.isEmpty()) {

            return "redirect:/pcmatrix";
        }
        CustomerInfo customerInfo = storeInfo.getCustomerInfo();

        CustomerForm customerForm = new CustomerForm(customerInfo);

        model.addAttribute("customerForm", customerForm);

        return "pcmatrixCustomer";
    }

    // POST: Save customer information.

    @RequestMapping(value = { "/pcmatrixCustomer" }, method = RequestMethod.POST)
    public String pcmatrixCustomerSave(HttpServletRequest request, //
                                           Model model, //
                                           @ModelAttribute("customerForm") @Validated CustomerForm customerForm, //
                                           BindingResult result, //
                                           final RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            customerForm.setValid(false);
            // Forward to reenter customer info.
            return "pcmatrixCustomer";
        }

        customerForm.setValid(true);
        StoreInfo storeInfo = Utils.getStoreInSession(request);
        CustomerInfo customerInfo = new CustomerInfo(customerForm);
        storeInfo.setCustomerInfo(customerInfo);

        return "redirect:/pcmatrixConfirm";
    }

    // GET: Show information to confirm.

    @RequestMapping(value = { "/pcmatrixConfirm" }, method = RequestMethod.GET)
    public String pcmatrixConfirmReview(HttpServletRequest request, Model model) {
        StoreInfo storeInfo = Utils.getStoreInSession(request);

        if (storeInfo.isEmpty()) {

            return "redirect:/pcmatrix";
        } else if (storeInfo.isValidCustomer()) {

            return "redirect:/pcmatrixCustomer";
        }
        model.addAttribute("myStore", storeInfo);

        return "pcmatrixConfirm";
    }

    // POST: Submit Cart (Save)
    @RequestMapping(value = { "/pcmatrixConfirm" }, method = RequestMethod.POST)

    public String pcmatrixConfirmSave(HttpServletRequest request, Model model) {
        StoreInfo storeInfo = Utils.getStoreInSession(request);

        if (storeInfo.isEmpty()) {

            return "redirect:/pcmatrix";
        } else if (storeInfo.isValidCustomer()) {

            return "redirect:/pcmatrixCustomer";
        }
        try {
            orderDAO.saveOrder(storeInfo);
        } catch (Exception e) {

            return "pcmatrixConfirm";
        }

        // Remove Store from Session.
        Utils.removpcmatrixInSession(request);

        // Store last cart.
        Utils.storeLastOrderedStoreInSession(request, storeInfo);

        return "redirect:/pcmatrixEnd";
    }

    @RequestMapping(value = { "/pcmatrixEnd" }, method = RequestMethod.GET)
    public String pcmatrixEnd(HttpServletRequest request, Model model) {

        StoreInfo lastOrderedStore = Utils.getLastOrderedStoreInSession(request);

        if (lastOrderedStore == null) {
            return "redirect:/pcmatrix";
        }
        model.addAttribute("lastOrderedStore", lastOrderedStore);
        return "pcmatrixEnd";
    }

    @RequestMapping(value = { "/productImage" }, method = RequestMethod.GET)
    public void productImage(HttpServletRequest request, HttpServletResponse response, Model model,
                             @RequestParam("code") String code) throws IOException {
        Product product = null;
        if (code != null) {
            product = this.productDAO.findProduct(code);
        }
        if (product != null && product.getImage() != null) {
            response.setContentType("image/jpeg, image/jpg, image/png, image/gif");
            response.getOutputStream().write(product.getImage());
        }
        response.getOutputStream().close();
    }

}