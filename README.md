# Discord IPC
Kotlin 2.2.10 (Java 21) library for interacting with locally running Discord instance without the use of JNI.  
The library is tested on Windows and macOS.

Changed activity creation and added the ability to add buttons and get a user avatar via a link.

## Credits 
Meteor Development for original Discord IPC

## Gradle
```groovy
repositories {

}

dependencies {
    implementation ""
    implementation "com.google.code.gson:gson:2.8.9" // GSON is not included but required
}
```

## Examples

### Create rpc

```kotlin
RichPresence.apply {
    appId = 1337L // required - your app id

    details = "Details" // optional
    state = "State" // optional
    
    largeImage = "large" to "LARGE TEXT" // optional
    smallImage = "https://i.imgur.com/example.png/" to "small text" // optional
    
    button1 = "button 1" to "https://example.ru/" // optional
    button2 = "button 2" to "https://example.net/"  // optional
}
```

### Update rpc
```kotlin
RichPresence.apply {
    details = "New Details"
}
```

### Get user info

```kotlin
RichPresence.user.let {
    println("Name: ${it.username}, Id: ${it.id}, Avatar Link: ${it.avatarLink()}")
}

RichPresence.user.let {
    println("Name: ${it.username}, Id: ${it.id}, Avatar Link: ${it.avatarLink(size = 512, forcePng = true)}")
}
```

### Drafts

```kotlin
enum class State(val details: String, val state: String) {
    Hello("hello", "world!"),
    Kowk("Mew", "Meow")
}

// ... 

RichPresence.apply { appId = 1093053626198523935L }

State.entries.forEach {
    RichPresence.saveDraft {
        draftId = it // as Draft Id you can use any type

        details = "Draft ${it.details}"
        state = "Draft ${it.state}"
    }
}

/*
   According to Discord rules, Rich Presence can be updated no more than once every 5-15 seconds.
   If you update it more frequently, the status will be cached and wait for the next event to be sent.
*/
sleep(6000)
RichPresence.setDraft(State.Hello)

sleep(6000)
RichPresence.setDraft(State.Kowk)

// If you want the drafts to change automatically in a cycle
RichPresence.startRolling()

// Disable rolling
RichPresence.stopRolling()
```

### Force stop

```kotlin
RichPresence.stop()
```


