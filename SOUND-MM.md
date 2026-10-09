# Temperature / Rainfall / Vegetation အသံ

ဒီ update မှာ signal ၃ မျိုးကို မတူတဲ့ measurement နဲ့ အသံထုတ်ပေးပါတယ်။

- Temperature: temperature anomaly (°C) များလေ pitch မြင့်လေ ဖြစ်ပါတယ်။
- Rainfall: rainfall (mm) များလေ noise နဲ့ ရေစက် pulse ပိုများလေ ဖြစ်ပါတယ်။ 0 mm ဆို အသံတိတ်ပါတယ်။
- Vegetation: NDVI များလေ pitch နဲ့ chord ပါဝင်မှု တိုးလာပါတယ်။

Rainfall / vegetation ကို temperature data ကနေ ခန့်မှန်းထားတာ မဟုတ်ပါ။
ဒီ ZIP ထဲမှာ NASA rainfall / NDVI download မပါပါ။
သင့် dataset မှ mm / NDVI တန်ဖိုးတွေကို API ထဲပေးရပါမယ်။
အောက်က 20 mm၊ 0.7 NDVI၊ array values တွေက စမ်းသပ်ဖို့ ဥပမာတန်ဖိုးတွေသာ ဖြစ်ပါတယ်။

## ဖွင့်ရန်

ZIP ထဲက `Earth-Beat` folder ကိုဖြည်ပြီး IntelliJ မှာ `pom.xml` ဖွင့်ပါ။
သင့် မူလ MySQL `earthDb` နဲ့ `application.properties` ကို ဆက်သုံးနိုင်ပါတယ်။
Java 25 ကို ရွေးပြီး Maven Reload လုပ်ပါ။

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

`EarthBeatApplication` / `EarthJukeboxApplication` နှစ်ခုလုံးက မှန်ကန်တဲ့ backend ကို ဖွင့်ပေးပါတယ်။
မူလ temperature endpoints တွေကို ဆက်သုံးနိုင်ပါတယ်။

## Browser မှာ အသံနားထောင်ရန်

Browser address bar ထဲ အောက်ပါ URL ကိုထည့်ပါ။ WAV player ပေါ်လာလျှင် Play နှိပ်ပါ။
အသံဖိုင် download ဖြစ်လျှင် အဲဒီ WAV ဖိုင်ကို ဖွင့်နားထောင်နိုင်ပါတယ်။

| အသံ | URL |
| --- | --- |
| Temperature (NASA database ထဲက တစ်နှစ်စာ) | `http://localhost:8080/api/earth/temperature/audio?year=2024` |
| Temperature (NASA database ထဲက တစ်လစာ) | `http://localhost:8080/api/earth/temperature/audio?year=2024&month=1` |
| Temperature (ကိုယ်ပေးသော anomaly) | `http://localhost:8080/api/earth/sound/audio?signal=temperature&value=1.25` |
| Rainfall | `http://localhost:8080/api/earth/rainfall/audio?rainfallMm=20` |
| Vegetation | `http://localhost:8080/api/earth/vegetation/audio?ndvi=0.7` |

တစ်လ / တစ်တန်ဖိုးစာ အသံက ပုံမှန် 450 ms ဖြစ်ပါတယ်။
အပြောင်းအလဲကို နားထောင်ရန် အောက်က timeline API ကိုသုံးပါ။

## JSON data ကြည့်ရန်

- `GET /api/earth/temperature?year=2024&month=1`: မူလ NASA temperature နဲ့ pitch။
- `GET /api/earth/rainfall?rainfallMm=20`: rainfall signal frame။
- `GET /api/earth/vegetation?ndvi=0.7`: vegetation signal frame။
- `GET /api/earth/sound/frame?signal=temperature&value=1.25`: ကိုယ်ပေးတဲ့ temperature signal။

Frame မှာ `frequencyHz`, `durationMs`, `waveform`, `intensity`, `pulseRateHz`,
`normalizedValue`, `clamped` ပါပါတယ်။
`waveform=noise` ကို OscillatorNode မှာ တိုက်ရိုက်မထည့်ပါနှင့်။ WAV endpoint ကိုသုံးနိုင်ပါတယ်။

## တန်ဖိုးအများကြီးကို အစဉ်လိုက် အသံထုတ်ရန်

`POST http://localhost:8080/api/earth/sound/audio`

Header: `Content-Type: application/json`

Rainfall:

```json
{"signal":"rainfall","values":[0,5,15,30,60,100,60,30,15,5,0]}
```

Vegetation:

```json
{"signal":"vegetation","values":[0.1,0.2,0.35,0.5,0.7,0.9,0.7,0.5,0.3,0.1]}
```

Temperature:

```json
{"signal":"temperature","values":[-0.6,-0.2,0.2,0.5,0.8,1.1,1.3,1.6]}
```

Response က WAV ဖိုင်ဖြစ်ပါတယ်။ Postman ရဲ့ `Send and Download` နဲ့ `.wav` အဖြစ်သိမ်းပါ။
JSON frames ပဲလိုလျှင် တူညီတဲ့ body နဲ့ `POST /api/earth/sound/frames` ကိုသုံးပါ။

Frontend မှ POST audio ကို play လုပ်ရန်:

```javascript
async function playSignal(signal, values) {
  const response = await fetch("http://localhost:8080/api/earth/sound/audio", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ signal, values })
  });
  if (!response.ok) throw new Error(await response.text());
  const url = URL.createObjectURL(await response.blob());
  const player = new Audio(url);
  const release = () => URL.revokeObjectURL(url);
  player.addEventListener("ended", release, { once: true });
  player.addEventListener("error", release, { once: true });
  try { await player.play(); } catch (error) { release(); throw error; }
  return player;
}
// Play button နှိပ်ချိန်မှာ ခေါ်ပါ။
```

## Input / mapping

- Rainfall: mm >= 0။ အသံ mapping reference က 0–100 mm။ 100 ထက်ပိုလျှင် မူရင်း measurement ကိုဆက်ပြပြီး အသံကို maximum မှာထားပါတယ်။
- Vegetation: NDVI -1 မှ 1 အတွင်း။ အသံအတွက် 0–1 ကိုသုံးပါတယ်။ အနုတ် NDVI ကို non-vegetated အနိမ့်ဆုံးအသံအဖြစ် ထားပါတယ်။
- Temperature: မူလ `earth.sound.*` configuration နဲ့ -0.6°C မှ 1.6°C mapping ကိုဆက်သုံးပါတယ်။
- Timeline: တစ်ကြိမ်မှာ value 1–120 ခု၊ WAV တစ်ဖိုင်အများဆုံး 60 seconds။
- WAV: 22050 Hz၊ 16-bit PCM၊ mono။ အသံစ / အသံဆုံး fade ပါပါတယ်။
- Invalid input ကို HTTP 400 ပြန်ပါတယ်။ NASA temperature month မရှိလျှင် မူလ 404 / 503 behavior အတိုင်းပြန်ပါတယ်။

အသံက measurement ကို နားထောင်လို့ရအောင် ပြောင်းထားသော sonification ဖြစ်ပါတယ်။
တကယ့်ရာသီဥတုအသံ recordings မဟုတ်ပါ။
