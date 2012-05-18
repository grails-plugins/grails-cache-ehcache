import java.util.concurrent.ConcurrentHashMap

import javax.annotation.PostConstruct

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable

class MessageService {

	private final Map<String, Message> messages = new ConcurrentHashMap<String, Message>()

	@Cacheable('message')
	Message getMessage(String title) {
		println 'Fetching message'
		return messages.get(title)
	}

	@CacheEvict(value='message', key='message.title')
	void save(Message message) {
		println "Saving message $message"
		messages.put(message.title, message)
	}

	Collection<Message> findAll() { messages.values() }

	@PostConstruct
	void addSomeDefaultMessages() {
		save new Message(title: 'Hello', body: 'Hello World')
		save new Message(title: 'Appointment', body: 'Remember the milk!')
	}
}
