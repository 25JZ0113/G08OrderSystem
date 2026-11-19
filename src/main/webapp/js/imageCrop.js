/*
 * メニュー管理画面:商品画像を選んで 4:3 に切り取る
 * 選んだ画像(JPG・PNG・WebP、10MB まで)を枠の中に表示し、ドラッグで位置・スライダーで大きさを決める。
 * 枠の中を 1024×768 の JPG にして、送信用の <input type="file" name="imageFile"> に入れる
 * (変換はブラウザがするので、サーバーは JPG だけを受け取ればよい)。
 */
(function () {
  'use strict';

  const OUT_W = 1024;
  const OUT_H = 768;
  const MAX_BYTES = 10 * 1024 * 1024;
  const TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  const picker  = document.getElementById('imagePicker');
  const upload  = document.getElementById('imageFile');
  const nameBox = document.getElementById('imageUrl');
  const crop    = document.getElementById('imageCrop');
  const canvas  = document.getElementById('cropCanvas');
  const zoom    = document.getElementById('cropZoom');
  const cancel  = document.getElementById('cropCancel');
  const preview = document.getElementById('imagePreview');
  if (!picker || !canvas) {
    return;
  }
  const g = canvas.getContext('2d');

  let img = null;       // 選んだ画像
  let cx = 0, cy = 0;   // 枠の中心に来る、画像上の位置(画像のピクセル)
  let previewUrl = null;
  let timer = 0;
  let ready = false;    // 送信用の JPG が今の枠と同じか
  const originalPreview = preview.innerHTML;

  /* 画像を枠いっぱい(cover)に表示したときの倍率 × スライダーの倍率 */
  function scale() {
    const cover = Math.max(canvas.width / img.naturalWidth, canvas.height / img.naturalHeight);
    return cover * Number(zoom.value);
  }

  /* 枠の外に画像の端が見えないように、中心の位置を収める */
  function clamp() {
    const s = scale();
    const halfW = canvas.width / s / 2;
    const halfH = canvas.height / s / 2;
    cx = Math.min(Math.max(cx, halfW), img.naturalWidth - halfW);
    cy = Math.min(Math.max(cy, halfH), img.naturalHeight - halfH);
  }

  /* 指定した大きさのキャンバスに、枠の中を描く */
  function drawTo(ctx, w, h) {
    const s = scale() * (w / canvas.width);
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, w, h);
    ctx.imageSmoothingQuality = 'high';
    ctx.drawImage(img, w / 2 - cx * s, h / 2 - cy * s, img.naturalWidth * s, img.naturalHeight * s);
  }

  function draw() {
    clamp();
    ready = false;
    drawTo(g, canvas.width, canvas.height);
    // 動かし終わってから少し待って、送信用の JPG を作る
    clearTimeout(timer);
    timer = setTimeout(makeJpeg, 150);
  }

  /* 枠の中を 1024×768 の JPG にして、送信用の input に入れる。右のプレビューも差し替える */
  function makeJpeg(done) {
    clearTimeout(timer);
    if (!img) {
      return;
    }
    const out = document.createElement('canvas');
    out.width = OUT_W;
    out.height = OUT_H;
    drawTo(out.getContext('2d'), OUT_W, OUT_H);
    out.toBlob(function (blob) {
      if (!img || !blob) {
        return;
      }
      const dt = new DataTransfer();
      dt.items.add(new File([blob], 'image.jpg', { type: 'image/jpeg' }));
      upload.files = dt.files;
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      previewUrl = URL.createObjectURL(blob);
      preview.innerHTML = '';
      const p = document.createElement('img');
      p.src = previewUrl;
      p.alt = '';
      preview.appendChild(p);
      ready = true;
      if (done) {
        done();
      }
    }, 'image/jpeg', 0.85);
  }

  /* ファイル名の欄:拡張子を .jpg にする。空なら元のファイル名(半角英数字にできれば)を入れる */
  function fillName(file) {
    const v = nameBox.value.trim();
    if (v) {
      nameBox.value = v.replace(/\.(jpe?g|png|webp)$/i, '') + '.jpg';
      return;
    }
    const base = file.name.replace(/\.[^.]*$/, '').toLowerCase()
        .replace(/[^a-z0-9._-]+/g, '-').replace(/^-+|-+$/g, '');
    if (/[a-z0-9]/.test(base)) {
      nameBox.value = base + '.jpg';
    }
  }

  function reset() {
    img = null;
    clearTimeout(timer);
    upload.value = '';
    picker.value = '';
    crop.hidden = true;
    nameBox.required = false;
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      previewUrl = null;
    }
    preview.innerHTML = originalPreview;
  }

  picker.addEventListener('change', function () {
    const file = picker.files[0];
    if (!file) {
      return;
    }
    if (!TYPES.includes(file.type)) {
      alert('JPG・PNG・WebP の画像を選んでください。');
      picker.value = '';
      return;
    }
    if (file.size > MAX_BYTES) {
      alert('画像が大きすぎます(10MB まで)。');
      picker.value = '';
      return;
    }
    const url = URL.createObjectURL(file);
    const loaded = new Image();
    loaded.onload = function () {
      URL.revokeObjectURL(url);
      img = loaded;
      zoom.value = 1;
      cx = img.naturalWidth / 2;
      cy = img.naturalHeight / 2;
      crop.hidden = false;
      nameBox.required = true;   // 画像を送るときはファイル名がいる
      fillName(file);
      draw();
      canvas.focus();
    };
    loaded.onerror = function () {
      URL.revokeObjectURL(url);
      alert('画像を読み込めませんでした。');
      picker.value = '';
    };
    loaded.src = url;
  });

  zoom.addEventListener('input', function () {
    if (img) {
      draw();
    }
  });

  cancel.addEventListener('click', reset);

  /* ドラッグで位置を動かす(画像を指でつかんで動かす向き) */
  let dragging = null;
  canvas.addEventListener('pointerdown', function (e) {
    if (!img) {
      return;
    }
    dragging = { x: e.clientX, y: e.clientY };
    canvas.setPointerCapture(e.pointerId);
  });
  canvas.addEventListener('pointermove', function (e) {
    if (!dragging) {
      return;
    }
    // 画面上の 1px が、キャンバス・画像の何ピクセルにあたるか
    const k = canvas.width / canvas.getBoundingClientRect().width / scale();
    cx -= (e.clientX - dragging.x) * k;
    cy -= (e.clientY - dragging.y) * k;
    dragging = { x: e.clientX, y: e.clientY };
    draw();
  });
  ['pointerup', 'pointercancel'].forEach(function (t) {
    canvas.addEventListener(t, function () {
      dragging = null;
    });
  });

  /* 矢印キーでも動かせる(Shift で大きく) */
  canvas.addEventListener('keydown', function (e) {
    const d = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] }[e.key];
    if (!img || !d) {
      return;
    }
    e.preventDefault();
    const step = (e.shiftKey ? 40 : 8) / scale();
    cx -= d[0] * step;
    cy -= d[1] * step;
    draw();
  });

  /* 送信する直前に、今の枠の JPG ができているか確かめる(動かしてすぐ押したときは、作ってから送る) */
  document.getElementById('productForm').addEventListener('submit', function (e) {
    if (!img || ready) {
      return;
    }
    const submitter = e.submitter;
    if (submitter && submitter.value === 'productDelete') {
      return;
    }
    e.preventDefault();
    makeJpeg(function () {
      e.target.requestSubmit(submitter);
    });
  });
})();
