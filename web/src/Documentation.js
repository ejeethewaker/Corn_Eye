// Documentation
// Displays the CornEye process documentation: application overview and ML training pipeline.
import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { database } from './firebase';
import { ref, get } from 'firebase/database';
import './Documentation.css';
import './Dashboard.css';

function Documentation() {
  const navigate = useNavigate();
  const [adminName, setAdminName] = useState('');
  const [adminInitials, setAdminInitials] = useState('');

  useEffect(() => {
    const loadAdmin = async () => {
      try {
        const storedEmail = localStorage.getItem('adminEmail') || sessionStorage.getItem('adminEmail') || '';
        // Show cached name instantly
        const cachedName = localStorage.getItem('adminCachedName');
        const cachedInitials = localStorage.getItem('adminCachedInitials');
        if (cachedName) {
          setAdminName(cachedName);
          setAdminInitials(cachedInitials || 'A');
        }
        const adminsRef = ref(database, 'admins');
        const adminsSnap = await get(adminsRef);
        if (adminsSnap.exists()) {
          const admins = adminsSnap.val();
          const matched = Object.values(admins).find((a) => a.email === storedEmail);
          if (matched) {
            const name = matched.fullName || 'Admin';
            const initials = name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2);
            setAdminName(name);
            setAdminInitials(initials);
            localStorage.setItem('adminCachedName', name);
            localStorage.setItem('adminCachedInitials', initials);
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
    localStorage.removeItem('adminCachedName');
    localStorage.removeItem('adminCachedInitials');
    sessionStorage.removeItem('adminLoggedIn');
    sessionStorage.removeItem('adminEmail');
    navigate('/');
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

      {/* Main Content */}
      <main className="dashboard-main doc-main">
        <h1 className="dashboard-title">Documentation</h1>

        {/* ── Application Overview ── */}
        <section className="doc-section">
          <h2 className="doc-section-title">Application Overview</h2>
          <p className="doc-text">
            CornEye helps corn farmers find out if their plant is sick — just by taking a photo with their phone.
            The app works completely offline, so farmers in remote areas can use it without internet.
          </p>

          <div className="doc-card-grid">
            <div className="doc-card">
              <div className="doc-card-icon doc-icon-blue">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="5" y="2" width="14" height="20" rx="2" ry="2"/>
                  <line x1="12" y1="18" x2="12.01" y2="18"/>
                </svg>
              </div>
              <h3>Mobile App (Android)</h3>
              <p>The farmer's app. Take a photo of a corn leaf and get an instant result — healthy or diseased. Built with Kotlin and works without internet.</p>
            </div>
            <div className="doc-card">
              <div className="doc-card-icon doc-icon-orange">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M2 12h20"/>
                  <path d="M12 2a15.3 15.3 0 014 10 15.3 15.3 0 01-4 10 15.3 15.3 0 01-4-10 15.3 15.3 0 014-10z"/>
                </svg>
              </div>
              <h3>Web Dashboard (React)</h3>
              <p>The admin's panel. View scan activity, manage farmer accounts, see disease statistics, and send notifications.</p>
            </div>
            <div className="doc-card">
              <div className="doc-card-icon doc-icon-green">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                  <path d="M2 17l10 5 10-5"/>
                  <path d="M2 12l10 5 10-5"/>
                </svg>
              </div>
              <h3>ML Training Script</h3>
              <p>A Python script that teaches the model to recognize corn diseases. It produces a small model file that the phone app uses.</p>
            </div>
          </div>
        </section>

        {/* ── Disease Classes ── */}
        <section className="doc-section">
          <h2 className="doc-section-title">Disease Classes (5)</h2>
          <p className="doc-text">The model can identify 5 types of results. Three are corn diseases, one means the leaf is healthy, and one catches non-corn images.</p>
          <div className="doc-table-wrapper">
            <table className="doc-table">
              <thead>
                <tr>
                  <th>Index</th>
                  <th>Class Name</th>
                  <th>What It Means</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>0</td>
                  <td>Common Rust</td>
                  <td>Small reddish-brown spots scattered across the leaf surface.</td>
                </tr>
                <tr>
                  <td>1</td>
                  <td>Gray Leaf Spot</td>
                  <td>Rectangular gray-brown patches that follow the leaf veins.</td>
                </tr>
                <tr>
                  <td>2</td>
                  <td>Healthy</td>
                  <td>The leaf looks normal — no disease found.</td>
                </tr>
                <tr>
                  <td>3</td>
                  <td>Northern Leaf Blight</td>
                  <td>Long, cigar-shaped gray-green marks on the leaf.</td>
                </tr>
                <tr>
                  <td>4</td>
                  <td>Invalid</td>
                  <td>Not a corn leaf — the photo shows something else, so the app rejects it.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        {/* ── Mobile Inference Pipeline ── */}
        <section className="doc-section">
          <h2 className="doc-section-title">Mobile Inference Pipeline</h2>
          <p className="doc-text">
            When a user takes or uploads a photo, the Android app runs a 3-step pipeline before showing results:
          </p>
          <div className="doc-steps">
            <div className="doc-step">
              <div className="doc-step-number">1</div>
              <div className="doc-step-content">
                <h4>Green Pixel Check</h4>
                <p>Before running the model, the app quickly checks if the photo has enough green color. If it doesn't look like a leaf at all (e.g. a wall or a hand), it gets rejected right away — no need to waste processing power.</p>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">2</div>
              <div className="doc-step-content">
                <h4>Model Scan</h4>
                <p>The photo is resized to 224&times;224 pixels and fed into the MobileNetV2 model. The model looks at the image and gives 5 scores — one for each possible result (3 diseases, healthy, or invalid). These scores add up to 100%.</p>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">3</div>
              <div className="doc-step-content">
                <h4>Result Filtering</h4>
                <p>Even after the model gives a result, the app double-checks it. The result is rejected if:</p>
                <ul>
                  <li>The top prediction is <strong>"Invalid"</strong> (not a corn leaf)</li>
                  <li>The confidence score is <strong>below 70%</strong> (the model isn't sure enough)</li>
                  <li>The model seems confused — its scores are spread too evenly across multiple classes (entropy &gt; 0.8)</li>
                </ul>
              </div>
            </div>
          </div>
        </section>

        {/* ── How the Model Is Created — Step by Step ── */}
        <section className="doc-section">
          <h2 className="doc-section-title">How the Model Is Created — Step by Step</h2>

          <div className="doc-steps">
            <div className="doc-step">
              <div className="doc-step-number">1</div>
              <div className="doc-step-content">
                <h4>Collect &amp; Label Images</h4>
                <p>We use two sources of data. First, the <strong>PlantVillage dataset</strong> — a free, publicly available collection of leaf photos. The script picks out the <strong>4 corn folders</strong> (Common Rust, Gray Leaf Spot, Healthy, Northern Leaf Blight) and groups <strong>all the other plant photos</strong> (tomato, potato, grape, etc.) into a 5th class called "Invalid." Second, we collected <strong>469 real-world field photos</strong> taken with a phone camera in actual corn fields (Gray Leaf Spot, Healthy, Northern Leaf Blight). These are automatically split 80% for training and 20% for validation and merged with the PlantVillage data.</p>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">2</div>
              <div className="doc-step-content">
                <h4>Balance the Classes</h4>
                <p>If one class has way more photos than another, the model would get biased toward the bigger class. To fix this, the script <strong>copies (oversamples)</strong> images from smaller classes until every class has roughly the same amount. The Invalid class is capped so it doesn't overwhelm the corn classes.</p>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">3</div>
              <div className="doc-step-content">
                <h4>Prepare &amp; Augment the Data</h4>
                <p>Every image is resized to <strong>224&times;224 pixels</strong>. During training, the script applies random changes to each image every time it's shown — so the model never sees the exact same picture twice. This forces it to learn general patterns instead of memorizing specific photos. The random changes include:</p>
                <ul>
                  <li>Flipping the image left-right or upside-down</li>
                  <li>Rotating it by 90°, 180°, or 270°</li>
                  <li>Randomly cropping and resizing it</li>
                  <li>Changing brightness, contrast, and color slightly</li>
                  <li>Adding small random noise (like camera grain)</li>
                </ul>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">4</div>
              <div className="doc-step-content">
                <h4>Build the Model Architecture</h4>
                <p>We use a pre-trained model called <strong>MobileNetV2</strong>. It was already trained on 1.4 million photos so it already understands shapes, colors, and textures. We remove its original output layer (which was made for 1,000 categories) and add our own custom layers that output just 5 results — our 5 corn classes. Think of it like replacing the brain's "answer sheet" while keeping all its learned knowledge.</p>
                <div className="doc-architecture-flow">
                  <span className="doc-arch-block">Input (224&times;224&times;3)</span>
                  <span className="doc-arch-arrow">&rarr;</span>
                  <span className="doc-arch-block">Rescale [0,1] to [-1,1]</span>
                  <span className="doc-arch-arrow">&rarr;</span>
                  <span className="doc-arch-block">MobileNetV2 Backbone</span>
                  <span className="doc-arch-arrow">&rarr;</span>
                  <span className="doc-arch-block">GlobalAveragePooling</span>
                  <span className="doc-arch-arrow">&rarr;</span>
                  <span className="doc-arch-block">BatchNorm</span>
                  <span className="doc-arch-arrow">&rarr;</span>
                  <span className="doc-arch-block">Dense(256, ReLU)</span>
                  <span className="doc-arch-arrow">&rarr;</span>
                  <span className="doc-arch-block">Dropout(50%)</span>
                  <span className="doc-arch-arrow">&rarr;</span>
                  <span className="doc-arch-block">Dense(128, ReLU)</span>
                  <span className="doc-arch-arrow">&rarr;</span>
                  <span className="doc-arch-block">Dropout(30%)</span>
                  <span className="doc-arch-arrow">&rarr;</span>
                  <span className="doc-arch-block doc-arch-output">Dense(5, Softmax)</span>
                </div>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">5</div>
              <div className="doc-step-content">
                <h4>Phase 2a — Train the New Layers Only</h4>
                <p>First, we <strong>freeze</strong> the MobileNetV2 backbone — meaning its weights stay locked and don't change. Only our newly added layers learn from the corn images. This runs for up to <strong>40 rounds (epochs)</strong>. The system automatically:</p>
                <ul>
                  <li><strong>Saves the best version</strong> whenever accuracy improves</li>
                  <li><strong>Slows down learning</strong> if progress stalls for 3 rounds</li>
                  <li><strong>Stops early</strong> if there's no improvement for 8 rounds (to prevent wasting time)</li>
                </ul>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">6</div>
              <div className="doc-step-content">
                <h4>Phase 2b — Fine-Tune the Backbone</h4>
                <p>Now we <strong>unlock the top 100 layers</strong> of MobileNetV2 so they can also adjust to corn-specific features (like rust textures or lesion shapes). We use a <strong>much smaller learning rate</strong> (10× lower) so we don't ruin what the model already knows. This trains for up to <strong>35 more rounds</strong>. At the end, the best model from both phases is loaded.</p>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">7</div>
              <div className="doc-step-content">
                <h4>Test the Model</h4>
                <p>We test the model on images it has <strong>never seen during training</strong> (the validation set). The goal is <strong>above 95% accuracy</strong> — the current model achieved <strong>99.43%</strong>. We also check:</p>
                <ul>
                  <li>A <strong>confusion matrix</strong> — a table that shows which diseases get mixed up with each other</li>
                  <li><strong>Precision, recall, and F1 score</strong> — these tell us how well the model performs on each individual class, not just overall</li>
                </ul>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">8</div>
              <div className="doc-step-content">
                <h4>Shrink the Model (INT8 Quantization)</h4>
                <p>The trained model is converted to <strong>TFLite format</strong> and <strong>INT8 quantization</strong> is applied, which means:</p>
                <ul>
                  <li>We feed 500 sample images through the model to measure value ranges at each layer</li>
                  <li>We convert the model's numbers from 32-bit (detailed) to 8-bit (compact) using those ranges</li>
                  <li>This shrinks the file to <strong>~3.0 MB</strong></li>
                  <li>We verify the smaller model still has <strong>above 94% accuracy</strong> (current: <strong>99.40%</strong>) — if not, we keep the bigger version</li>
                </ul>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">9</div>
              <div className="doc-step-content">
                <h4>Save &amp; Deploy</h4>
                <p>The script saves these files:</p>
                <ul>
                  <li><code>corn_disease_model.tflite</code> — the small model that runs on Android</li>
                  <li><code>labels.txt</code> — a list of class names so the app knows what each number means</li>
                  <li><code>corn_disease_keras_model.keras</code> — the full model, saved in case we need to retrain later</li>
                  <li>Training logs (CSV files) for reviewing what happened during training</li>
                </ul>
                <p>The <code>.tflite</code> and <code>labels.txt</code> files are <strong>automatically copied</strong> into the Android app's assets folder, so the next time the app is built, it uses the updated model.</p>
              </div>
            </div>
          </div>
        </section>

        {/* ── Pipeline Summary ── */}
        <section className="doc-section">
          <h2 className="doc-section-title">Pipeline Summary</h2>
          <div className="doc-pipeline-flow">
            <div className="doc-pipeline-item">
              <div className="doc-pipeline-icon doc-icon-blue">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
              </div>
              <span>Raw Images</span>
            </div>
            <span className="doc-pipeline-arrow">&rarr;</span>
            <div className="doc-pipeline-item">
              <div className="doc-pipeline-icon doc-icon-orange">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="16 3 21 3 21 8"/>
                  <line x1="4" y1="20" x2="21" y2="3"/>
                  <polyline points="21 16 21 21 16 21"/>
                  <line x1="15" y1="15" x2="21" y2="21"/>
                  <line x1="4" y1="4" x2="9" y2="9"/>
                </svg>
              </div>
              <span>Balance &amp; Augment</span>
            </div>
            <span className="doc-pipeline-arrow">&rarr;</span>
            <div className="doc-pipeline-item">
              <div className="doc-pipeline-icon doc-icon-green">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                  <path d="M2 17l10 5 10-5"/>
                  <path d="M2 12l10 5 10-5"/>
                </svg>
              </div>
              <span>Train Head (40 ep)</span>
            </div>
            <span className="doc-pipeline-arrow">&rarr;</span>
            <div className="doc-pipeline-item">
              <div className="doc-pipeline-icon doc-icon-purple">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="3"/>
                  <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>
                </svg>
              </div>
              <span>Fine-Tune (35 ep)</span>
            </div>
            <span className="doc-pipeline-arrow">&rarr;</span>
            <div className="doc-pipeline-item">
              <div className="doc-pipeline-icon doc-icon-red">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </div>
              <span>Evaluate (&gt;95%)</span>
            </div>
            <span className="doc-pipeline-arrow">&rarr;</span>
            <div className="doc-pipeline-item">
              <div className="doc-pipeline-icon doc-icon-teal">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="4" y="4" width="16" height="16" rx="2" ry="2"/>
                  <rect x="9" y="9" width="6" height="6"/>
                  <line x1="9" y1="1" x2="9" y2="4"/>
                  <line x1="15" y1="1" x2="15" y2="4"/>
                  <line x1="9" y1="20" x2="9" y2="23"/>
                  <line x1="15" y1="20" x2="15" y2="23"/>
                  <line x1="20" y1="9" x2="23" y2="9"/>
                  <line x1="20" y1="14" x2="23" y2="14"/>
                  <line x1="1" y1="9" x2="4" y2="9"/>
                  <line x1="1" y1="14" x2="4" y2="14"/>
                </svg>
              </div>
              <span>Quantize INT8</span>
            </div>
            <span className="doc-pipeline-arrow">&rarr;</span>
            <div className="doc-pipeline-item">
              <div className="doc-pipeline-icon doc-icon-blue">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="5" y="2" width="14" height="20" rx="2" ry="2"/>
                  <line x1="12" y1="18" x2="12.01" y2="18"/>
                </svg>
              </div>
              <span>Deploy to Android</span>
            </div>
          </div>
          <p className="doc-text" style={{marginTop: '16px', textAlign: 'center', color: '#666'}}>
            The final model is only ~3.5 MB, runs in under a second on a phone, and detects corn leaf diseases with over 95% accuracy — all completely offline, no internet needed.
          </p>
        </section>

        {/* ── Model Architecture ── */}
        <section className="doc-section">
          <h2 className="doc-section-title">Model Architecture Summary</h2>
          <p className="doc-text">A quick reference of the model's key specs.</p>
          <div className="doc-table-wrapper">
            <table className="doc-table">
              <tbody>
                <tr><td className="doc-table-label">Base Model</td><td>MobileNetV2 — a lightweight model pre-trained on 1.4M images</td></tr>
                <tr><td className="doc-table-label">Input Size</td><td>224 × 224 pixels, 3 color channels (RGB)</td></tr>
                <tr><td className="doc-table-label">Output</td><td>5 scores (one per class) that add up to 100%</td></tr>
                <tr><td className="doc-table-label">Quantization</td><td>INT8 — model numbers compressed from 32-bit to 8-bit for smaller size</td></tr>
                <tr><td className="doc-table-label">Training Data</td><td>PlantVillage dataset + 469 real-world field photos (Gray Leaf Spot, Healthy, Northern Leaf Blight)</td></tr>
                <tr><td className="doc-table-label">Validation Accuracy</td><td>99.43%</td></tr>
                <tr><td className="doc-table-label">Quantized Accuracy</td><td>99.40% (INT8)</td></tr>
                <tr><td className="doc-table-label">File Format</td><td>TensorFlow Lite (.tflite) — made for mobile devices</td></tr>
              </tbody>
            </table>
          </div>
        </section>

        {/* ── Firebase Functions Backend ── */}
        <section className="doc-section">
          <h2 className="doc-section-title">Firebase Functions Backend</h2>
          <p className="doc-text">
            CornEye uses two Firebase Cloud Functions (Node.js 20, us-central1) to handle the admin
            forgot-password OTP flow. These run server-side so that Gmail credentials are never
            exposed to the browser.
          </p>

          <div className="doc-card-grid">
            <div className="doc-card">
              <div className="doc-card-icon doc-icon-blue">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
              </div>
              <h3>sendOtp</h3>
              <p>Receives the admin's email, verifies it exists in the <code>/admins</code> node, generates a 6-digit OTP, stores it at <code>/otps/&lt;emailKey&gt;</code> with a 10-minute expiry, then sends a styled HTML email via Gmail SMTP (Nodemailer).</p>
            </div>
            <div className="doc-card">
              <div className="doc-card-icon doc-icon-green">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </div>
              <h3>verifyOtpAndReset</h3>
              <p>Receives the email, OTP, and new password. Checks the OTP hasn't expired and matches the stored value. If valid, it updates <code>/admins/&lt;id&gt;/password</code> and deletes the OTP record so it can't be reused.</p>
            </div>
          </div>

          <div className="doc-steps" style={{ marginTop: '24px' }}>
            <div className="doc-step">
              <div className="doc-step-number">1</div>
              <div className="doc-step-content">
                <h4>Admin clicks "Forgot Password?"</h4>
                <p>The login page shows an email entry form. When submitted, it calls the <code>sendOtp</code> Firebase Function via an HTTPS callable request.</p>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">2</div>
              <div className="doc-step-content">
                <h4>OTP email is sent</h4>
                <p>The function generates a random 6-digit code, saves it to Firebase Realtime Database with a 10-minute expiry, and sends it to the admin's registered email using Gmail SMTP. The Gmail App Password is stored in a <code>.env</code> file on the server — never in the browser.</p>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">3</div>
              <div className="doc-step-content">
                <h4>Admin enters the OTP</h4>
                <p>A 6-box input appears on screen. The admin types or pastes the code. A 60-second countdown shows how long is left, with a resend option once it expires.</p>
              </div>
            </div>
            <div className="doc-step">
              <div className="doc-step-number">4</div>
              <div className="doc-step-content">
                <h4>Password is reset</h4>
                <p>After entering the OTP, the admin sets a new password. The <code>verifyOtpAndReset</code> function validates the OTP, updates the password in Firebase, and deletes the OTP record. The admin is then returned to the login screen.</p>
              </div>
            </div>
          </div>
        </section>

        {/* ── Admin Account Management ── */}
        <section className="doc-section">
          <h2 className="doc-section-title">Admin Account Management</h2>
          <p className="doc-text">
            The admin web dashboard includes account control features that let administrators
            manage farmer access in real time.
          </p>

          <div className="doc-card-grid">
            <div className="doc-card">
              <div className="doc-card-icon doc-icon-orange">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="7" r="4"/>
                  <path d="M5.5 21a6.5 6.5 0 0113 0"/>
                </svg>
              </div>
              <h3>Account Deactivation Toggle</h3>
              <p>On the User Profile page, admins can switch any farmer account between <strong>Active</strong> and <strong>Deactivated</strong>. The change is written to <code>/farmers/&lt;id&gt;/status</code> in Firebase immediately.</p>
            </div>
            <div className="doc-card">
              <div className="doc-card-icon doc-icon-blue">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0110 0v4"/>
                </svg>
              </div>
              <h3>Login Enforcement</h3>
              <p>At login, the mobile app always fetches the account status directly from the server (bypassing any local cache). If the status is not <code>"active"</code>, login is blocked with a clear message.</p>
            </div>
            <div className="doc-card">
              <div className="doc-card-icon doc-icon-green">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M18 8h1a4 4 0 010 8h-1"/>
                  <path d="M2 8h16v9a4 4 0 01-4 4H6a4 4 0 01-4-4V8z"/>
                  <line x1="6" y1="1" x2="6" y2="4"/>
                  <line x1="10" y1="1" x2="10" y2="4"/>
                  <line x1="14" y1="1" x2="14" y2="4"/>
                </svg>
              </div>
              <h3>Real-Time Session Kick</h3>
              <p>If a farmer is already inside the app when their account is deactivated, a real-time Firebase listener on the <code>HomeScreen</code> detects the status change instantly and redirects them to the login screen, clearing their local session.</p>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

export default Documentation;
