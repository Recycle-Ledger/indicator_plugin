## 사용준비

### AndroidManifest.xml

블루투스 설정을 넣어줍니다.

```
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" android:maxSdkVersion="30" />

    <application
      ...
```

### pubspec.yaml

permission_handler 패키지를 임포트합니다.
퍼미션의 경우 플러그인에서 자체적으로 지원하지 않기 때문에 권한 추가 과정은 따로 추가해야합니다.

```
dependencies:
  permission_handler: ^11.0.1
```

## 사용법

```
// 1. Indicator 플러그인을 초기화해줍니다.   
final _indicatorPlugin = IndicatorPlugin();
// 2. 값을 수신받고 원하는 값에 할당해서 사용합니다. 최초 연결시 페어링 창이 뜹니다. 비밀번호 1234
_indicatorPlugin.startScan().listen((event) {
      setState(() {
        weight = event;
      });
    });
// 3. 더이상 값이 필요없을 경우 dispose 해줍니다.
_indicatorPlugin.dispose();
```