# Discord IPC
Kotlin 2.2.0 (Java 21) library for interacting with locally running Discord instance without the use of JNI.  
The library is tested on Windows and macOS.

Changed activity creation and added the ability to add buttons and get a user avatar via a link.

## Gradle
```groovy
repositories {

}

dependencies {
    implementation ""
    implementation "com.google.code.gson:gson:2.8.9" // GSON is not included but required
}
```

## Credits 
Meteor Development for original Discord IPC

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
    println("Name: ${it.username}, Id: ${it.id}, Avatar Link: ${it.avatarLink}")
}
```

### Force stop

```kotlin
RichPresence.stop()
```


