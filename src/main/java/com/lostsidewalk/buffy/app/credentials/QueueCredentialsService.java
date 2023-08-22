package com.lostsidewalk.buffy.app.credentials;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.queue.QueueCredential;
import com.lostsidewalk.buffy.queue.QueueCredentialDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class for managing queue credentials. This class provides methods for adding, updating, and deleting
 * queue credentials, as well as finding credentials by queue Id.
 */
@Slf4j
@Service
public class QueueCredentialsService {

    @Autowired
    QueueCredentialDao queueCredentialDao;

    /**
     * Adds a new queue credential with the specified parameters.
     *
     * @param username       The username associated with the credential.
     * @param queueId        The Id of the queue to which the credential belongs.
     * @param basicUsername  The basic authentication username.
     * @param basicPassword  The basic authentication password.
     * @throws DataAccessException  If there is an issue accessing the data.
     * @throws DataUpdateException  If there is an issue updating the data.
     */
    public void addCredential(String username, Long queueId, String basicUsername, String basicPassword)
            throws DataAccessException, DataUpdateException {
        QueueCredential queueCredential = QueueCredential.from(username, queueId, basicUsername, basicPassword);
        queueCredentialDao.add(queueCredential);
    }

    /**
     * Finds and returns a list of queue credentials associated with the given username and queue Id.
     *
     * @param username  The username associated with the credentials.
     * @param queueId   The Id of the queue for which credentials are to be retrieved.
     * @return A list of queue credentials.
     * @throws DataAccessException If there is an issue accessing the data.
     */
    public List<QueueCredential> findByQueueId(String username, Long queueId) throws DataAccessException {
        return queueCredentialDao.findByQueueId(username, queueId);
    }

    /**
     * Updates the password for a queue credential with the specified parameters.
     *
     * @param username       The username associated with the credential.
     * @param queueId        The Id of the queue to which the credential belongs.
     * @param basicUsername  The basic authentication username.
     * @param basicPassword  The new basic authentication password.
     * @throws DataAccessException  If there is an issue accessing the data.
     * @throws DataUpdateException  If there is an issue updating the data.
     */
    public void updatePassword(String username, Long queueId, String basicUsername, String basicPassword)
            throws DataAccessException, DataUpdateException {
        queueCredentialDao.updatePassword(username, queueId, basicUsername, basicPassword);
    }

    /**
     * Deletes a queue credential with the specified parameters.
     *
     * @param username       The username associated with the credential.
     * @param queueId        The Id of the queue to which the credential belongs.
     * @param basicUsername  The basic authentication username.
     * @throws DataAccessException  If there is an issue accessing the data.
     * @throws DataUpdateException  If there is an issue updating the data.
     */
    public void deleteQueueCredential(String username, Long queueId, String basicUsername)
            throws DataAccessException, DataUpdateException {
        queueCredentialDao.deleteByBasicUsername(username, queueId, basicUsername);
    }
}
