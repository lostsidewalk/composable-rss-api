package com.lostsidewalk.buffy.app.credentials;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
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
     * @param username       The username associated with the queue.
     * @param queueId        The Id of the queue to which the credential belongs.
     * @param basicUsername  The basic authentication username.
     * @param basicPassword  The basic authentication password.
     * @return The id of the new queue credential object.
     * @throws DataAccessException  If there is an issue accessing the data.
     * @throws DataUpdateException  If there is an issue updating the data.
     * @throws DataConflictException If there is a duplicate key.
     */
    public final Long addCredential(String username, Long queueId, String basicUsername, String basicPassword)
            throws DataAccessException, DataUpdateException, DataConflictException {
        QueueCredential queueCredential = QueueCredential.from(username, queueId, basicUsername, basicPassword);
        return queueCredentialDao.add(queueCredential);
    }

    /**
     * Finds a single queue credential by Id.
     *
     * @param username         The username associated with the queue.
     * @param queueId          The Id of the queue for which credentials are to be retrieved.
     * @param credentialId     The Id of the credential to fetch.
     * @return a queue credential object.
     * @throws DataAccessException if there is an issue accessing the data.
     */
    public final QueueCredential findById(String username, Long queueId, Long credentialId) throws DataAccessException {
        return queueCredentialDao.findById(username, queueId, credentialId);
    }

    /**
     * Finds and returns a list of queue credentials associated with the given username and queue Id.
     *
     * @param username  The username associated with the queue.
     * @param queueId   The Id of the queue for which credentials are to be retrieved.
     * @return A list of queue credentials.
     * @throws DataAccessException If there is an issue accessing the data.
     */
    public final List<QueueCredential> findByQueueId(String username, Long queueId) throws DataAccessException {
        return queueCredentialDao.findByQueueId(username, queueId);
    }

    /**
     * Updates the password for a queue credential with the specified parameters.
     *
     * @param username       The username associated with the queue.
     * @param queueId        The Id of the queue to which the credential belongs.
     * @param credentialId   The Id of the credential object to update.
     * @param basicPassword  The new basic authentication password.
     * @throws DataAccessException  If there is an issue accessing the data.
     * @throws DataUpdateException  If there is an issue updating the data.
     */
    public final void updatePassword(String username, Long queueId, Long credentialId, String basicPassword)
            throws DataAccessException, DataUpdateException {
        queueCredentialDao.updatePassword(username, queueId, credentialId, basicPassword);
    }

    /**
     * Deletes all queue credentials from the queue given by queueId.
     *
     * @param username       The username associated with the queue.
     * @param queueId        The Id of the queue to which the credential belongs.
     * @throws DataAccessException  If there is an issue accessing the data.
     * @throws DataUpdateException  If there is an issue updating the data.
     */
    public final void deleteQueueCredentials(String username, Long queueId)
            throws DataAccessException, DataUpdateException {
        queueCredentialDao.deleteByQueueId(username, queueId);
    }

    /**
     * Deletes a queue credential with the specified parameters.
     *
     * @param username       The username associated with the queue.
     * @param queueId        The Id of the queue to which the credential belongs.
     * @param credentialId   The Id of the credential object to delete.
     * @throws DataAccessException  If there is an issue accessing the data.
     * @throws DataUpdateException  If there is an issue updating the data.
     */
    public final void deleteQueueCredential(String username, Long queueId, Long credentialId)
            throws DataAccessException, DataUpdateException {
        queueCredentialDao.deleteById(username, queueId, credentialId);
    }

    @Override
    public final String toString() {
        return "QueueCredentialsService{" +
                "queueCredentialDao=" + queueCredentialDao +
                '}';
    }
}
