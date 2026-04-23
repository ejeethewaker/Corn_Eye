// DocumentationQuestions
// Possible questions for the CornEye capstone demo & defense.
import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { database } from './firebase';
import { ref, get } from 'firebase/database';
import './DocumentationQuestions.css';
import './Dashboard.css';

const QA_SECTIONS = [
  {
    title: 'Project Overview',
    items: [
      {
        q: 'What problem does CornEye solve?',
        a: 'Corn farmers in the Philippines can lose 30–40% of their harvest because diseases go unnoticed until it\'s too late. CornEye lets them take a photo of a corn leaf with their phone and get an instant diagnosis — no internet and no expert visit needed. This way, they can treat the plant early and save more of their crop.',
      },
      {
        q: 'Who are the target users?',
        a: 'Smallholder corn farmers and agricultural workers in the Philippines. The app is built for basic Android phones commonly used in rural farming areas.',
      },
      {
        q: 'What are the 5 classes your model detects?',
        a: 'Common Rust, Gray Leaf Spot, Healthy, Northern Leaf Blight, and Invalid. The first three are corn diseases, "Healthy" means the leaf is fine, and "Invalid" means the photo isn\'t a corn leaf at all.',
      },
      {
        q: 'Why did you include an "Invalid" class instead of just the 4 corn classes?',
        a: 'Without it, the model would be forced to pick one of the 4 corn classes for any image — even a photo of a shoe or a tomato leaf. The Invalid class teaches the model to recognize "this is not a corn leaf" and reject it, so farmers only get results for actual corn photos.',
      },
      {
        q: 'What is the scope and limitation of CornEye?',
        a: 'CornEye can only detect 3 specific corn diseases and identify healthy leaves. It cannot diagnose nutrient problems, pest damage, or diseases it wasn\'t trained on. It also works best with clear, well-lit photos — blurry or very dark images may give wrong results.',
      },
    ],
  },
  {
    title: 'Dataset & Data Preparation',
    items: [
      {
        q: 'Where did the dataset come from?',
        a: 'We used the PlantVillage dataset, which is a free, publicly available collection of over 50,000 leaf photos covering 38 different crop diseases. We took the 4 corn folders for our disease classes, and used all the other plant folders (tomato, potato, grape, etc.) as the Invalid class.',
      },
      {
        q: 'How many images are in the training set?',
        a: 'After balancing, each of the 5 classes has roughly the same number of images — around 2,000+ per corn class. The Invalid class is limited to 2× the biggest corn class so it doesn\'t overpower the others.',
      },
      {
        q: 'Why did you need class balancing?',
        a: 'The non-corn images greatly outnumber the corn images. If we didn\'t balance them, the model would learn to just predict "Invalid" for everything because that would give the highest overall score. By making all classes equal in size, the model learns each disease properly.',
      },
      {
        q: 'What data augmentation did you apply and why?',
        a: 'During training, we randomly flip, rotate, crop, change brightness, and add noise to images. This is done because the PlantVillage photos are very clean and consistent, but real photos from a farmer\'s phone will be taken outdoors in different lighting and angles. Augmentation makes the model more flexible and accurate on real-world photos.',
      },
      {
        q: 'Did you split the data into train/validation/test?',
        a: 'Yes. The PlantVillage dataset comes with separate training and validation folders. We train the model using the training set and measure how well it performs using the validation set (images it has never seen during training). The quantized model is also tested on a separate 500-image sample.',
      },
    ],
  },
  {
    title: 'Machine Learning & Model Architecture',
    items: [
      {
        q: 'Why MobileNetV2 instead of ResNet, EfficientNet, or a custom CNN?',
        a: 'MobileNetV2 was specifically built for phones. It\'s tiny (~3.5 MB) and fast (~50ms per photo) while still being accurate. In comparison, ResNet50 is ~100 MB and EfficientNet-B0 is ~20 MB. Since our app runs on budget Android phones with no internet, small size and speed matter the most.',
      },
      {
        q: 'What is transfer learning and why did you use it?',
        a: 'Instead of training a model from scratch (which needs millions of images), we start with MobileNetV2 that was already trained on 1.4 million photos. It already knows how to recognize shapes, edges, textures, and colors. We just teach it the final step: "based on what you see, is this Common Rust, Gray Leaf Spot, etc.?" This saves time and gives much better results with our limited dataset.',
      },
      {
        q: 'Explain the two-phase training strategy.',
        a: 'Phase 1 (Head Only): We lock the pre-trained part of the model and only train our new output layers for up to 30 rounds. This teaches the new layers how to use the existing features for our 5 corn classes. Phase 2 (Fine-Tuning): We unlock the top 60 layers of the model and train everything together for 25 more rounds at a much slower learning rate. This lets the model fine-tune its understanding to focus on corn-specific details like rust spots or lesion shapes.',
      },
      {
        q: 'Why freeze the backbone first?',
        a: 'The new layers we added start with random values. If we train everything at once, those random values would send chaotic signals back into the pre-trained part and mess it up. By freezing the backbone first, we let the new layers learn and stabilize. Only then do we carefully fine-tune the backbone with small, controlled updates.',
      },
      {
        q: 'What optimizer and loss function did you use?',
        a: 'We used the Adam optimizer, which automatically adjusts how fast each part of the model learns. For the loss function, we used categorical cross-entropy — it heavily punishes the model when it\'s confidently wrong. This is important because a wrong diagnosis (like saying a diseased leaf is "Healthy") could cost a farmer their crop.',
      },
      {
        q: 'What callbacks did you use during training?',
        a: 'Three automatic helpers: (1) ModelCheckpoint saves the model every time it hits a new best accuracy, (2) ReduceLROnPlateau slows down learning if the model stops improving for 3 rounds, and (3) EarlyStopping completely stops training if there\'s no improvement for 8 rounds, so we don\'t waste time.',
      },
      {
        q: 'What is the role of Dropout in your model?',
        a: 'During training, Dropout randomly turns off 50% of neurons in one layer and 30% in another. This forces the model to not depend on any single neuron and makes it better at handling new images it hasn\'t seen before. Think of it like studying for an exam by covering random notes — it makes you learn the concept, not just memorize answers.',
      },
      {
        q: 'What is BatchNormalization and why did you add it?',
        a: 'BatchNormalization adjusts the numbers flowing through the network so they stay in a consistent range. Without it, the numbers can fluctuate wildly which makes training unstable and slow. With it, the model trains faster and more reliably.',
      },
      {
        q: 'What accuracy did the model achieve?',
        a: 'The goal is above 95% accuracy on the validation set for the full model, and above 94% for the smaller quantized version. If the quantized model drops below 94%, the system automatically keeps the larger (more accurate) version instead.',
      },
    ],
  },
  {
    title: 'TFLite Conversion & Quantization',
    items: [
      {
        q: 'What is TFLite and why do you need it?',
        a: 'TFLite (TensorFlow Lite) is a version of TensorFlow made for phones. A normal TensorFlow model needs a huge library (~500 MB) that can\'t fit in a phone app. TFLite uses a tiny ~2 MB runtime, so the model can run directly inside the Android app.',
      },
      {
        q: 'What is INT8 quantization?',
        a: 'Normally, the model stores numbers as 32-bit floats (very detailed). INT8 quantization converts them to 8-bit integers (smaller but slightly less precise). This makes the model about 4× smaller — for example, from 14 MB down to 3.5 MB. It also runs faster on phones that don\'t have a powerful GPU.',
      },
      {
        q: 'What is a representative dataset?',
        a: 'To convert 32-bit numbers to 8-bit without losing too much accuracy, the system needs to know what range of values each layer typically produces. So we feed 500 sample images (~100 from each class) through the model during conversion. These samples "represent" normal inputs and help calibrate the conversion.',
      },
      {
        q: 'Why did you keep float32 input and output?',
        a: 'The phone camera gives us float numbers for pixels, and we need float numbers back to check confidence scores. Keeping the input and output as float32 means the Android app code stays simple — no extra conversion steps. The internal layers still use the faster INT8 format.',
      },
      {
        q: 'What happens if quantization fails or drops accuracy?',
        a: 'After quantization, the script tests the smaller model on 500 images. If accuracy drops below 94%, it automatically falls back to the larger float32 version, which is bigger but guaranteed to be accurate. Safety first.',
      },
    ],
  },
  {
    title: 'Mobile App & On-Device Inference',
    items: [
      {
        q: 'How does the mobile app process a photo?',
        a: 'Three steps: (1) Green check — the app quickly looks at the photo\'s colors. If there\'s not enough green, it\'s probably not a leaf and gets rejected. (2) Model scan — the photo is resized and fed to the model, which returns 5 scores. (3) Result filter — the app rejects the result if it says "Invalid," if the confidence is below 70%, or if the model seems confused.',
      },
      {
        q: 'What is the green pixel ratio check?',
        a: 'Before running the model (which takes more processing power), the app does a quick color check. It counts how many pixels in the image are green. If there aren\'t enough green pixels, it\'s clearly not a leaf photo — so the app rejects it immediately, saving time and battery.',
      },
      {
        q: 'Why use a confidence threshold of 70%?',
        a: 'If the model\'s highest score is below 70%, it means the model isn\'t sure enough about its answer. Showing an uncertain diagnosis could mislead a farmer into applying the wrong treatment. The 70% threshold makes sure we only show results the model is reasonably confident about.',
      },
      {
        q: 'What is entropy and why use 0.8 as the threshold?',
        a: 'Entropy measures how "spread out" the model\'s scores are. If the model gives almost equal scores to all 5 classes, it means it\'s confused and can\'t decide. That\'s high entropy. If entropy is above 0.8, the result is rejected because the model clearly can\'t tell what it\'s looking at.',
      },
      {
        q: 'Does the app need an internet connection?',
        a: 'No — the diagnosis works 100% offline. The model runs entirely on the phone. The only thing that uses internet is syncing scan history to the admin dashboard (Firebase), and that\'s optional.',
      },
      {
        q: 'What happens with the scan results?',
        a: 'Results are saved on the phone first. When the phone has internet, they get synced to Firebase so the admin dashboard can show scan history, disease statistics, and farmer activity. Admins can also send notifications to farmers based on what they see.',
      },
    ],
  },
  {
    title: 'Web Admin Dashboard',
    items: [
      {
        q: 'What is the purpose of the web dashboard?',
        a: 'It\'s a control panel for admins (like agricultural workers or researchers). They can see all farmer scan results, check disease statistics and trends, manage farmer accounts, and send notifications — all in real time.',
      },
      {
        q: 'What tech stack does the web app use?',
        a: 'React 19 for the user interface, React Router 7 for page navigation, Firebase Realtime Database for storing and retrieving data in real time, Firebase Authentication for admin login, and Vercel for hosting.',
      },
      {
        q: 'How is authentication handled?',
        a: 'Admins log in with their email and password, which is checked against the "admins" list in Firebase. If "Remember Me" is checked, the login is saved permanently. Otherwise, it only lasts for that browser session. Every page checks if the user is logged in — if not, they\'re redirected to the login page.',
      },
      {
        q: 'How can an admin reset their password?',
        a: 'There is a Forgot Password link on the login page. Clicking it starts a 4-step OTP flow: enter email → receive a 6-digit code by email (sent by a Firebase Cloud Function) → enter the code within 10 minutes → set a new password. The code is validated server-side and deleted after a successful reset.',
      },
    ],
  },
  {
    title: 'Evaluation & Testing',
    items: [
      {
        q: 'How did you evaluate the model\'s performance?',
        a: 'We tested it on images it was never trained on (the validation set). We checked overall accuracy, plus a confusion matrix that shows exactly which classes the model confuses with each other. We also calculated precision, recall, and F1 score for each of the 5 classes individually.',
      },
      {
        q: 'What is precision vs recall in this context?',
        a: 'Precision answers: "When the model says it\'s Common Rust, how often is it actually Common Rust?" Recall answers: "Out of all actual Common Rust images, how many did the model correctly find?" High recall is especially important here — missing a real disease (saying it\'s healthy when it\'s not) is more dangerous than a false alarm.',
      },
      {
        q: 'How did you test the TFLite model separately?',
        a: 'We have a separate script (test_tflite.py) that loads the small .tflite model and runs it on validation images. We compare its results to the full Keras model to make sure nothing was lost or broken during the conversion and quantization process.',
      },
      {
        q: 'Did you do any real-world field testing?',
        a: 'Yes, in two ways: (1) we uploaded photos of corn leaves found online through the app to check if it classifies correctly, and (2) we went to actual corn fields and took photos of real leaves with a phone camera. The augmentation we used during training (changing brightness, cropping, adding noise) was designed to prepare the model for these real-world conditions.',
      },
    ],
  },
  {
    title: 'Technical Deep Dives',
    items: [
      {
        q: 'What is depthwise separable convolution (MobileNetV2)?',
        a: 'In a normal convolution, the model looks at all color channels at once for each filter. Depthwise separable convolution splits this into two simpler steps: (1) look at each channel separately, then (2) combine the results with a small 1×1 filter. This does roughly the same job but uses 8–9× less computation — which is why MobileNetV2 is so fast on phones.',
      },
      {
        q: 'What is GlobalAveragePooling and why use it?',
        a: 'After MobileNetV2 processes an image, it produces a large grid of numbers (7×7×1280). GlobalAveragePooling takes the average of each channel, turning that big grid into just 1,280 numbers. This massively reduces the model\'s size and makes it less likely to overfit (memorize training data instead of learning patterns).',
      },
      {
        q: 'Why normalize images to [0, 1] then rescale to [-1, 1]?',
        a: 'First, we divide pixel values by 255 to get them into the [0, 1] range — this is standard practice. But MobileNetV2 was originally trained with images in the [-1, 1] range, so we add a rescaling step that converts [0, 1] to [-1, 1]. This way, the input matches what the pre-trained model expects.',
      },
      {
        q: 'How does the oversampling work exactly?',
        a: 'If one class has fewer images than the biggest class, we randomly copy (duplicate) images from the smaller class until it matches the bigger one. Since each duplicated image gets different random augmentations every training round (flips, rotations, etc.), the model doesn\'t just memorize the copies — it still learns general patterns.',
      },
      {
        q: 'What is the softmax function?',
        a: 'Softmax takes the raw numbers from the model\'s last layer and converts them into percentages that add up to 100%. For example, it might output [2%, 1%, 5%, 2%, 90%], which means the model is 90% confident the image is "Invalid." The class with the highest percentage becomes the prediction.',
      },
      {
        q: 'What is categorical cross-entropy loss?',
        a: 'It\'s how we measure how wrong the model is. If the correct answer is "Common Rust" and the model predicted 90% for Common Rust, the loss (error) is very small. But if it only predicted 10% for the correct answer, the loss is very high. The bigger the mistake, the bigger the penalty — this pushes the model to learn faster and more confidently.',
      },
    ],
  },
    {
    title: 'Admin Dashboard & Security',
    items: [
      {
        q: 'How does the admin forgot password flow work?',
        a: 'When an admin clicks "Forgot Password?", they enter their registered email. The app calls a Firebase Cloud Function (sendOtp) that checks if the email exists in the /admins node, generates a 6-digit OTP, stores it in /otps/<emailKey> with a 10-minute expiry, and sends it via Gmail SMTP using Nodemailer. The admin enters the code in a 6-box input on screen, then sets a new password which is verified and updated by a second Firebase Function (verifyOtpAndReset).',
      },
      {
        q: 'Why use Firebase Functions for sending the OTP email instead of doing it from the browser?',
        a: 'If we sent the email from the browser (e.g. using EmailJS), the Gmail credentials (username and App Password) would be visible to anyone who opens DevTools. Firebase Functions run server-side, so the credentials are stored in a .env file on the server and are never exposed to the client.',
      },
      {
        q: 'Where is the OTP stored and for how long?',
        a: 'The OTP is stored in the Firebase Realtime Database under /otps/<emailKey> along with an expiresAt timestamp set to 10 minutes from when it was generated. The verifyOtpAndReset function rejects any OTP where the current time is past expiresAt, preventing expired codes from being used.',
      },
      {
        q: 'How does account deactivation work from the admin side?',
        a: 'On the User Profile page, the admin flips a toggle next to "Account Status." This immediately writes status: "inactive" (or "active") to /farmers/<userId>/status in Firebase Realtime Database via an update() call. The change takes effect instantly — no server restart or delay.',
      },
      {
        q: 'How does the mobile app enforce account deactivation at login?',
        a: 'Instead of using addListenerForSingleValueEvent (which can return stale cached data), the login screen uses get().await() — a Kotlin coroutine-based call that always fetches fresh data from the Firebase server. After credentials match, a second get().await() call reads only the status field. If status is not "active", login is blocked and the user sees a message to contact the administrator.',
      },
      {
        q: 'What happens if a farmer is already inside the app when their account is deactivated?',
        a: 'The HomeScreen registers a real-time Firebase listener (addValueEventListener) on the farmer\'s status field when the screen opens. The moment an admin changes the status to "inactive", Firebase pushes that update to all connected clients. The listener fires, clears the local session (DataStore), and navigates the user back to the login screen — effectively kicking them out in real time.',
      },
      {
        q: 'Why did switching back to "active" still show an error before the fix?',
        a: 'The original login used addListenerForSingleValueEvent, which first checks Firebase\'s in-memory cache. After a deactivation event, the cache held "inactive". Even after the admin toggled it back to "active" in the database, the login read the stale cached value and still blocked the user. Switching to get().await() fixed this because it always goes directly to the server and ignores the cache.',
      },
    ],
  },
  {
    title: 'Challenges & Future Work',
    items: [
      {
        q: 'What challenges did you face during development?',
        a: 'Four main challenges: (1) The PlantVillage training photos look very different from real phone camera photos taken outside — we solved this with heavy augmentation. (2) The Invalid class had way more images than the corn classes — we fixed it with balancing. (3) The model was too big for budget phones — we used INT8 quantization to shrink it 4×. (4) Making sure the model actually learned patterns instead of just memorizing specific photos.',
      },
      {
        q: 'What would you improve if you had more time?',
        a: 'We would add more corn diseases and other crops, collect real field photos to improve training, add severity grading (mild/moderate/severe), explore ways to update the model on-device without redownloading it, and build a treatment recommendation feature that suggests what to do after a diagnosis.',
      },
      {
        q: 'Could this work for other crops?',
        a: 'Absolutely. The system is designed to be reusable. You would just swap the training data — for example, use tomato or potato folders from PlantVillage instead of corn — adjust the class names, and retrain. The whole training pipeline and app architecture stays the same.',
      },
      {
        q: 'How would you handle a new disease not in the training set?',
        a: 'Right now, a new disease would be wrongly classified as one of the existing classes or as Invalid. To add support for it, you would: (1) collect labeled photos of the new disease, (2) add a new class to the training setup, and (3) retrain the model. The training script is designed to be run again easily whenever new data is added.',
      },
    ],
  },
];

function DocumentationQuestions() {
  const navigate = useNavigate();
  const [adminName, setAdminName] = useState('');
  const [adminInitials, setAdminInitials] = useState('');
  const [openIndex, setOpenIndex] = useState(null);

  useEffect(() => {
    const loadAdmin = async () => {
      try {
        const storedEmail = localStorage.getItem('adminEmail') || sessionStorage.getItem('adminEmail') || '';
        const adminsRef = ref(database, 'admins');
        const adminsSnap = await get(adminsRef);
        if (adminsSnap.exists()) {
          const admins = adminsSnap.val();
          const matched = Object.values(admins).find((a) => a.email === storedEmail);
          if (matched) {
            const name = matched.fullName || 'Admin';
            setAdminName(name);
            setAdminInitials(name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2));
          }
        }
      } catch (err) {
        console.error('Failed to load admin data:', err);
      }
    };
    loadAdmin();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('adminLoggedIn');
    localStorage.removeItem('adminEmail');
    sessionStorage.removeItem('adminLoggedIn');
    sessionStorage.removeItem('adminEmail');
    navigate('/');
  };

  const toggleQuestion = (sectionIdx, itemIdx) => {
    const key = `${sectionIdx}-${itemIdx}`;
    setOpenIndex((prev) => (prev === key ? null : key));
  };

  return (
    <div className="dashboard-container">
      {/* Sidebar */}
      <aside className="dashboard-sidebar">
        <div className="sidebar-top">
          <div className="sidebar-brand">
            <img
              src={process.env.PUBLIC_URL + '/dashboard-logo.png'}
              alt="CornEye Logo"
              className="sidebar-brand-logo"
            />
          </div>

          <Link to="/profile" className="sidebar-user-card sidebar-user-clickable">
            <div className="user-avatar">{adminInitials || 'A'}</div>
            <div className="user-info">
              <span className="user-name">{adminName || 'Admin'}</span><span className="user-role">Administrator</span>
            </div>
          </Link>

          <nav className="sidebar-nav">
            <button className="nav-item" onClick={() => navigate('/dashboard')}>
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M3 9.5L12 3l9 6.5V20a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5z"/>
                  <path d="M9 21V12h6v9"/>
                </svg>
              </span>
              <span>Dashboard</span>
            </button>
            <button className="nav-item" onClick={() => navigate('/users')}>
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="7" r="4"/>
                  <path d="M5.5 21a6.5 6.5 0 0113 0"/>
                </svg>
              </span>
              <span>Users</span>
            </button>
            <button className="nav-item" onClick={() => navigate('/notifications')}>
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M18 8a6 6 0 10-12 0c0 7-3 9-3 9h18s-3-2-3-9"/>
                  <path d="M13.73 21a2 2 0 01-3.46 0"/>
                  <path d="M6 4a1 1 0 011-1"/>
                  <path d="M18 4a1 1 0 00-1-1"/>
                </svg>
              </span>
              <span>Notifications</span>
            </button>
          </nav>
        </div>

        <div className="sidebar-bottom">
          <button className="nav-item logout-btn" onClick={handleLogout}>
            <span className="nav-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
            </span>
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="doc-main">
        <section className="doc-section">
          <h1 style={{ fontSize: '28px', fontWeight: 800, color: '#1a1a1a', margin: '0 0 4px' }}>
            Defense &amp; Demo Questions
          </h1>
          <p className="doc-text" style={{ marginBottom: '32px' }}>
            Prepared Q&amp;A covering {QA_SECTIONS.reduce((sum, s) => sum + s.items.length, 0)} possible
            questions across {QA_SECTIONS.length} categories for the CornEye capstone defense.
          </p>
        </section>

        {QA_SECTIONS.map((section, sIdx) => (
          <section className="doc-section" key={sIdx}>
            <h2 className="doc-section-title">{section.title}</h2>
            <div className="qa-list">
              {section.items.map((item, iIdx) => {
                const isOpen = openIndex === `${sIdx}-${iIdx}`;
                return (
                  <div className={`qa-item ${isOpen ? 'qa-item-open' : ''}`} key={iIdx}>
                    <button className="qa-question" onClick={() => toggleQuestion(sIdx, iIdx)}>
                      <span className="qa-number">{iIdx + 1}</span>
                      <span className="qa-q-text">{item.q}</span>
                      <span className={`qa-chevron ${isOpen ? 'qa-chevron-open' : ''}`}>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <polyline points="6 9 12 15 18 9"/>
                        </svg>
                      </span>
                    </button>
                    {isOpen && (
                      <div className="qa-answer">
                        <p>{item.a}</p>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </section>
        ))}
      </main>
    </div>
  );
}

export default DocumentationQuestions;
