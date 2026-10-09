# Earth Information Jukebox — Backend

Temperature / rainfall / vegetation JSON နဲ့ WAV အသံ update ပါဝင်ပါတယ်။
Run နည်း၊ အသံ endpoint နဲ့ timeline request များကို `SOUND-MM.md` မှာ အရင်ဖတ်ပါ။
Rainfall / vegetation အတွက် ကိုယ့် dataset မှ mm / NDVI values ကို API ထဲပေးရပါတယ်။


Spring Boot 4.1.1၊ Java 25၊ MySQL၊ Spring Data JPA ကို သုံးထားပါတယ်။

## Project ထဲထည့်ရန်

1. ZIP ထဲက `Earth-Beat` folder ကို ဖြည်ပါ။
2. လက်ရှိ project root ထဲသို့ `pom.xml`၊ `src`၊ Maven wrapper ဖိုင်များနဲ့ `db` ကို ကူးထည့်ပါ။ `pom.xml` နဲ့ `application.properties` ကို ဒီ ZIP ထဲက ဖိုင်တွေနဲ့ အစားထိုးပါ။
3. Java package က `org.example.earthjukebox` ဖြစ်ပါတယ်။ `EarthBeatApplication` နဲ့ `EarthJukeboxApplication` နှစ်ခုလုံးက မှန်ကန်တဲ့ backend ကိုဖွင့်ပေးပါတယ်။
4. IntelliJ မှာ Project SDK နဲ့ Maven Runner JRE ကို Java 25 ရွေးပြီး Maven Reload လုပ်ပါ။

သီးခြား project အဖြစ်လည်း ဒီ folder ထဲက `pom.xml` ကို IntelliJ နဲ့ တိုက်ရိုက်ဖွင့်နိုင်ပါတယ်။

## Database

MySQL ဖွင့်ထားပြီး အောက်ပါ SQL ကို တစ်ကြိမ် run ပါ။ SQL ဖိုင်ကို `db/create-earth-db.sql` မှာလည်း ထည့်ထားပါတယ်။

```sql
CREATE DATABASE IF NOT EXISTS earthDb CHARACTER SET utf8mb4;
```

`src/main/resources/application.properties` မှာ connection ပြင်နိုင်ပါတယ်။ ပုံမှန် username က `root`၊ password က သင်ပေးထားတဲ့ `admin` ဖြစ်ပါတယ်။ `DB_USERNAME` နဲ့ `DB_PASSWORD` environment variable ရှိရင် အဲဒီတန်ဖိုးတွေကို သုံးပါမယ်။

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/earthDb
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:admin}
spring.jpa.hibernate.ddl-auto=update
```

JPA က `earth_temperature_month` နဲ့ `earth_temperature_dataset` table တွေကို ဖန်တီးပေးပါမယ်။

## Run

Windows Terminal / PowerShell မှာ project folder ထဲကနေ—

```powershell
.\mvnw.cmd spring-boot:run
```

macOS / Linux မှာ—

```bash
./mvnw spring-boot:run
```

App စတင်ချိန်မှာ NASA CSV ကို download လုပ်ပြီး transaction တစ်ခုနဲ့ database ထဲသိမ်းပါတယ်။ ပထမ import ပြီးသွားရင် log မှာ `NASA GISTEMP import complete` တွေ့ပါမယ်။

နေ့တိုင်း UTC 02:00 / မြန်မာအချိန် 08:30 မှာ data ကိုပြန်ယူပါတယ်။ ယခု importer ကို backend instance တစ်ခုမှာ run ဖို့ ရေးထားပါတယ်။

## API

| Method | URL | ပြန်ရမည့် data |
| --- | --- | --- |
| GET | `/api/earth/temperature?year=2024&month=1` | တစ်လစာ temperature နဲ့ pitch |
| GET | `/api/earth/temperature?year=2024` | ရွေးထားတဲ့နှစ်ရဲ့ ရရှိနိုင်သော လများ၊ metadata၊ mapping |
| GET | `/api/earth/temperature/years` | data ရှိတဲ့နှစ်စာရင်း |
| GET | `/api/earth/temperature/latest` | နောက်ဆုံးရရှိတဲ့ လတစ်လ |
| GET | `/api/earth/dataset` | NASA source၊ baseline၊ hash၊ import/download အချိန် |

Browser / Postman မှာ—

```text
http://localhost:8080/api/earth/temperature?year=2024&month=1
```

အောက်ပါ JSON သည် anomaly `1.25°C` အတွက် နမူနာဖြစ်ပါတယ်။ NASA က data ပြန်ပြင်လျှင် actual value က ကွာနိုင်ပါတယ်။

```json
{
  "year": 2024,
  "month": 1,
  "date": "2024-01",
  "temperatureAnomalyC": 1.25,
  "frequencyHz": 684,
  "durationMs": 450,
  "waveform": "sine",
  "clamped": false,
  "mappingVersion": "temperature-log-frequency-v1"
}
```

`postman/Earth-jukebox.postman_collection.json` ကို Postman ထဲ Import လုပ်ပြီးလည်း စမ်းနိုင်ပါတယ်။

## Frontend ချိတ်ရန်

ပုံမှန် CORS origin က `http://localhost:3000` နဲ့ `http://localhost:5173` ဖြစ်ပါတယ်။ Frontend URL ကိုပြောင်းထားရင် `earth.frontend.allowed-origins` မှာ URL အတိအကျ ထည့်ပါ။

တစ်နှစ်စာ API ကို တစ်ကြိမ်တောင်းပြီး `frames` ကို အစဉ်လိုက် play လုပ်နိုင်ပါတယ်။ Frontend က—

- `temperatureAnomalyC` ကို စာသား/ဂရပ်မှာပြပါတယ်။
- `date` သို့မဟုတ် `year` + `month` နဲ့ သက်ဆိုင်ရာ visualization frame ကိုရွေးပါတယ်။
- `frequencyHz`၊ `durationMs`၊ `waveform` ကို Web Audio API မှာသုံးပြီး user က Play နှိပ်ချိန် အသံဖန်တီးပါတယ်။

ဒီ ZIP မှာ CSV → MySQL → JSON API → pitch mapping နဲ့ WAV အသံထုတ်ခြင်း ပါပါတယ်။ EIC visual frame နဲ့ month တစ်ခုချင်းချိတ်ထားတဲ့ manifest နဲ့ frontend audio playback ကို နောက်တစ်ဆင့်တွင် ချိတ်ရပါမယ်။ `visualizationPageUrl` က source page link ဖြစ်ပြီး တစ်လချင်း frame timestamp မဟုတ်ပါ။

## Data နဲ့ အသံအဓိပ္ပာယ်

NASA GISTEMP v4 ရဲ့ global monthly temperature anomaly ကို ယူထားပါတယ်။ `°C` တန်ဖိုးက 1951–1980 ပျမ်းမျှအပူချိန်နဲ့ နှိုင်းယှဉ်ထားသော ကွာခြားချက်ဖြစ်ပါတယ်။

NASA CSV ရဲ့ ပထမစာကြောင်းက title၊ ဒုတိယစာကြောင်းက header ဖြစ်ပါတယ်။ `Jan` မှ `Dec` ကိုသာ လစဉ် data အဖြစ်ယူပါတယ်။ `J-D` က annual average ဖြစ်တာကြောင့် လတစ်လအဖြစ် မယူပါ။

`***` နဲ့ empty value တွေကို ကျော်ပြီး `0` အဖြစ် မသိမ်းပါ။ မရှိသေးတဲ့လကိုတောင်းရင် `404` ပြန်ပါတယ်။ ပထမ import မအောင်မြင်သေးလျှင် `503` ပြန်ပါတယ်။

CSV ပြန်ယူလျှင် တူညီတဲ့ နှစ်/လ record တွေ မပွားပါ။ NASA ပြန်ပြင်တဲ့ measurement ကို update လုပ်ပါတယ်။ Import မအောင်မြင်လျှင် နောက်ဆုံးအောင်မြင်ခဲ့တဲ့ database data ကို ဆက်သုံးပါတယ်။ `retrievedAt` က နောက်ဆုံးအောင်မြင်သော download အချိန်၊ `importedAt` က data နောက်ဆုံးပြောင်းသိမ်းခဲ့တဲ့ အချိန်ဖြစ်ပါတယ်။

Frequency သည် NASA ရဲ့ တိုင်းတာထားသောအသံ မဟုတ်ပါ။ ဒီ project ရဲ့ သတ်မှတ်ထားသော sonification mapping ဖြစ်ပါတယ်။

```text
t = clamp((anomalyC - minC) / (maxC - minC), 0, 1)
frequencyHz = round(minHz * (maxHz / minHz) ^ t)
```

Default mapping က `-0.6°C → 180Hz`၊ `1.6°C → 880Hz` ဖြစ်ပါတယ်။ Range အပြင်ထွက်လျှင် pitch ကို boundary မှာထားပြီး `clamped=true` ပြပါတယ်။ မူရင်း anomaly ကို မပြောင်းပါ။

## Tests

ပုံမှန် tests တွေက H2 ကို MySQL mode နဲ့သုံးပြီး NASA network ကို မတောင်းပါ။ H2 dependency က test scope သာဖြစ်လို့ app run ချိန်မှာ MySQL ကိုသုံးပါတယ်။

```powershell
.\mvnw.cmd clean verify
```

NASA CSV အစစ်ကို download လုပ်ပြီး database နဲ့ API အထိစစ်ရန်—

```powershell
.\mvnw.cmd "-Dtest=NasaLiveImportIT" test
```

Live check မှာလည်း H2 test database ကိုသာသုံးပါတယ်။ သင့် local MySQL instance နဲ့ connection ကို သင့်စက်မှာ app run ချိန်တွင် စစ်ရပါမယ်။

## Sources

- NASA GISTEMP: https://data.giss.nasa.gov/gistemp/
- NASA CSV: https://data.giss.nasa.gov/gistemp/tabledata_v4/GLB.Ts%2BdSST.csv
- NASA Climate Spiral / EIC visualization source: https://svs.gsfc.nasa.gov/5190/

