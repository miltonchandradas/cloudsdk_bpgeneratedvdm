package com.sap.cloud.sdk.tutorial.controllers;

import java.util.List;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.datamodel.odata.helper.Order;
import com.sap.cloud.sdk.tutorial.vdm.namespaces.businesspartner.BusinessPartner;
import com.sap.cloud.sdk.tutorial.vdm.services.DefaultAPIBUSINESSPARTNERService;

@RestController
public class BusinessPartnerController {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BusinessPartnerController.class);

    private static final String CATEGORY_PERSON = "1";
    private static final String APIKEY_HEADER = "apikey";

    @RequestMapping( value = "/getBusinessPartners", method = RequestMethod.GET )
    public String getBusinessPartners() {
        final String destinationName = "mydestination";
        final HttpDestination destination = DestinationAccessor.getDestination(destinationName).asHttp();

        final List<BusinessPartner> businessPartners =
                    new DefaultAPIBUSINESSPARTNERService().withServicePath("sap/opu/odata/sap/API_BUSINESS_PARTNER")
                            .getAllBusinessPartner()
                            .select(BusinessPartner.BUSINESS_PARTNER,
                                    BusinessPartner.LAST_NAME,
                                    BusinessPartner.FIRST_NAME,
                                    BusinessPartner.IS_MALE,
                                    BusinessPartner.IS_FEMALE,
                                    BusinessPartner.CREATION_DATE)
                            .filter(BusinessPartner.BUSINESS_PARTNER_CATEGORY.eq(CATEGORY_PERSON))
                            .orderBy(BusinessPartner.LAST_NAME, Order.ASC)
                            .top(200)
                            .withHeader(APIKEY_HEADER, System.getenv("SANDBOX_APIKEY"))
                            .executeRequest(destination);

            logger.info(String.format("Found %d business partner(s).", businessPartners.size()));

        return new Gson().toJson(businessPartners);
    }
    
}
