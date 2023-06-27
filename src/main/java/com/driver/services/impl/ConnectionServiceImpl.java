package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{

        User user = userRepository2.findById(userId).get();

        if(user.getConnected()) throw new Exception("Already connected");

        Country country = new Country();
        country.enrich(countryName);

        if(user.getOriginalCountry().getCountryName().equals(country.getCountryName())) return user;

        List<ServiceProvider> serviceProviderList = user.getServiceProviderList();

        int serviceProviderId = Integer.MAX_VALUE;
        ServiceProvider serviceProviderObj = null;

        for (ServiceProvider serviceProvider : serviceProviderList)
        {
            List<Country> countryList = serviceProvider.getCountryList();

            for (Country country1 : countryList)
            {
                if(country1.getCode().equals(country.getCode()))
                {
                    if(serviceProvider.getId() < serviceProviderId)
                    {
                        serviceProviderId = serviceProvider.getId();
                        serviceProviderObj = serviceProvider;
                    }
                }
            }
        }
        if(serviceProviderObj == null) throw new Exception("Unable to connect");

        Connection connection = new Connection();
        connection.setUser(user);
        user.setConnected(true);

        user.getConnectionList().add(connection);
        user.setMaskedIp(country.getCode()+"."+serviceProviderObj.getId()+"."+userId);

        connection.setServiceProvider(serviceProviderObj);

        serviceProviderObj.getConnectionList().add(connection);

        userRepository2.save(user);
        serviceProviderRepository2.save(serviceProviderObj);
        return user;
    }
    @Override
    public User disconnect(int userId) throws Exception {

        User user = userRepository2.findById(userId).get();
        if(! user.getConnected()) throw new Exception("Already disconnected");

        user.setConnected(false);
        user.setMaskedIp(null);

        user = userRepository2.save(user);

        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {

        User receiver = userRepository2.findById(receiverId).get();

        CountryName receiverCountryName = null;

        if(receiver.getConnected())
        {
            String maskedCode = receiver.getMaskedIp().substring(0,3);
            if(maskedCode.equals("001"))
            {
                receiverCountryName = CountryName.IND;
            }
            else if (maskedCode.equals("002")) {
                receiverCountryName = CountryName.USA;
            }
            else if (maskedCode.equals("003")) {
                receiverCountryName = CountryName.AUS;
            }
            else if (maskedCode.equals("004")) {
                receiverCountryName = CountryName.CHI;
            }
            else if (maskedCode.equals("005")) {
                receiverCountryName = CountryName.JPN;
            }

        }
        else receiverCountryName = receiver.getOriginalCountry().getCountryName();

        User user = null;

        try {
            user = connect(senderId,receiverCountryName.toString());
        }
        catch (Exception e)
        {
            throw new Exception("Cannot establish communication");
        }
        return user;
    }
}
