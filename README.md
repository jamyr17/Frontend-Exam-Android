Students: 
- Jamel Sandí
- Jamyr González

Intructiones:

- First, perform a Sync Now on the Android project and verify that everything is installed correctly.
- Second, build the project.
- And third, check if we have a connected mobile device and launch the app.

The project has the following manifest permissions:

<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

The project uses the following activities:

<activity  
    android:name=".MainActivity"  
    android:exported="true"  
    android:theme="@style/Theme.StudentCoursesSystem">  
    <intent-filter>  
        <action android:name="android.intent.action.MAIN" />  
        <category android:name="android.intent.category.LAUNCHER" />  
    </intent-filter>  
</activity>  

<activity  
    android:name=".StudentsActivity"  
    android:exported="false"  
    android:parentActivityName=".MainActivity">  
    <meta-data  
        android:name="android.support.PARENT_ACTIVITY"  
        android:value=".MainActivity" />  
</activity>  

<activity  
    android:name=".StudentDetailActivity"  
    android:exported="false"  
    android:parentActivityName=".StudentsActivity">  
    <meta-data  
        android:name="android.support.PARENT_ACTIVITY"  
        android:value=".StudentsActivity" />  
</activity>

The project uses the following service for Firebase Messaging:

<service  
    android:name=".services.MessagingService"  
    android:exported="false">  
    <intent-filter>  
        <action android:name="com.google.firebase.MESSAGING_EVENT" />  
    </intent-filter>  
</service>

The project uses the following plugin in build.gradle.kts:

// Google Services Plugin  
id("com.google.gms.google-services") version "4.4.2" apply false

The project includes the following libraries:

dependencies {  
    implementation("io.coil-kt:coil-compose:2.5.0") {  
        exclude(group = "org.jetbrains", module = "annotations")  
    }  

    implementation(libs.androidx.core.ktx)  
    implementation(libs.androidx.lifecycle.runtime.ktx)  
    implementation(libs.androidx.activity.compose)  
    implementation(platform(libs.androidx.compose.bom))  
    implementation(libs.androidx.ui)  
    implementation(libs.androidx.ui.graphics)  
    implementation(libs.androidx.ui.tooling.preview)  
    implementation(libs.androidx.material3)  
    implementation(libs.retrofit)  
    implementation(libs.converter.gson)  
    implementation(libs.androidx.lifecycle.viewmodel.compose)  
    implementation(libs.androidx.appcompat)  
    implementation("androidx.compose.material:material-icons-extended:1.6.0")  
    implementation("androidx.compose.material3:material3:1.1.0")  
    implementation("androidx.compose.material:material-icons-extended:1.5.0")  

    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")  

    // Firebase  
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))  
    implementation("com.google.firebase:firebase-messaging:24.1.1")  
    implementation("com.google.firebase:firebase-analytics:22.4.0")  

    testImplementation(libs.junit)  
    androidTestImplementation(libs.androidx.junit)  
    androidTestImplementation(libs.androidx.espresso.core)  
    androidTestImplementation(platform(libs.androidx.compose.bom))  
    androidTestImplementation(libs.androidx.ui.test.junit4)  
    debugImplementation(libs.androidx.ui.tooling)  
    debugImplementation(libs.androidx.ui.test.manifest)  
}
