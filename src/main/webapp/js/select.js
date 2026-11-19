/*
 * ドロップダウン(select.login-input)を、ページの中に一覧を描くドロップダウンに置き換える
 *
 * ブラウザ標準の一覧は位置や色を変えられず、画面の下の方では一覧が画面の外にはみ出していた。
 * 元の <select> は見えなくして残し(フォームではこちらの値が送られる)、
 * 代わりにボタンと一覧(listbox)を表示する。下に入りきらないときは上に開く。
 *
 * 操作:クリック/Enter・Space・↓ で開く。一覧では ↑↓・Home・End で移動、Enter・Space で決定、Esc で閉じる
 */
(function () {
  'use strict';

  let count = 0;

  function enhance(select) {
    count++;
    const wrap = document.createElement('span');
    wrap.className = 'select-wrap';
    select.parentNode.insertBefore(wrap, select);
    wrap.appendChild(select);

    // 元の select は見えなくする(値はこちらで送る)
    select.classList.add('visually-hidden');
    select.tabIndex = -1;
    select.setAttribute('aria-hidden', 'true');

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'login-input select-button';
    button.id = (select.id || 'select' + count) + '-button';
    button.setAttribute('aria-haspopup', 'listbox');
    button.setAttribute('aria-expanded', 'false');
    const label = select.id ? document.querySelector('label[for="' + select.id + '"]') : null;
    if (label) {
      label.htmlFor = button.id;
      // ラベルを押したらボタンに移るだけにする(そのままだとボタンが押されて一覧が開く)
      label.addEventListener('click', function (e) {
        e.preventDefault();
        button.focus();
      });
    } else if (select.getAttribute('aria-label')) {
      button.setAttribute('aria-label', select.getAttribute('aria-label'));
    }
    const text = document.createElement('span');
    text.className = 'select-button-text';
    button.appendChild(text);
    wrap.appendChild(button);

    const list = document.createElement('ul');
    list.className = 'select-list';
    list.id = button.id + '-list';
    list.setAttribute('role', 'listbox');
    list.hidden = true;
    button.setAttribute('aria-controls', list.id);
    if (label) {
      label.id = label.id || button.id + '-label';
      list.setAttribute('aria-labelledby', label.id);
    } else if (select.getAttribute('aria-label')) {
      list.setAttribute('aria-label', select.getAttribute('aria-label'));
    }
    Array.from(select.options).forEach(function (opt, i) {
      const li = document.createElement('li');
      li.className = 'select-option';
      li.setAttribute('role', 'option');
      li.tabIndex = -1;
      li.dataset.index = i;
      li.textContent = opt.textContent;
      if (opt.disabled) {
        li.setAttribute('aria-disabled', 'true');
      }
      list.appendChild(li);
    });
    wrap.appendChild(list);

    const items = Array.from(list.children);

    function sync() {
      const opt = select.options[select.selectedIndex];
      text.textContent = opt ? opt.textContent : '';
      items.forEach(function (li, i) {
        li.setAttribute('aria-selected', i === select.selectedIndex ? 'true' : 'false');
      });
    }

    function open() {
      if (!list.hidden) {
        return;
      }
      list.hidden = false;
      button.setAttribute('aria-expanded', 'true');
      wrap.classList.remove('opens-up');
      list.style.maxHeight = '';
      // 下に入りきらず、上の方が広いときは上に開く。どちらでも入りきらなければ、空いている広さまでにしてスクロールさせる
      const r = button.getBoundingClientRect();
      const below = window.innerHeight - r.bottom - 12;
      const above = r.top - 12;
      const up = list.offsetHeight > below && above > below;
      wrap.classList.toggle('opens-up', up);
      const room = up ? above : below;
      if (list.offsetHeight > room) {
        list.style.maxHeight = Math.max(room, 120) + 'px';
      }
      const current = items[select.selectedIndex] || items[0];
      if (current) {
        current.focus();
        current.scrollIntoView({ block: 'nearest' });
      }
    }

    function close(focusButton) {
      if (list.hidden) {
        return;
      }
      list.hidden = true;
      button.setAttribute('aria-expanded', 'false');
      if (focusButton) {
        button.focus();
      }
    }

    function choose(i) {
      if (items[i].getAttribute('aria-disabled') === 'true') {
        return;
      }
      if (select.selectedIndex !== i) {
        select.selectedIndex = i;
        select.dispatchEvent(new Event('change', { bubbles: true }));
      }
      sync();
      close(true);
    }

    function move(from, step) {
      let i = from;
      do {
        i += step;
      } while (i >= 0 && i < items.length && items[i].getAttribute('aria-disabled') === 'true');
      if (i >= 0 && i < items.length) {
        items[i].focus();
      }
    }

    button.addEventListener('click', function () {
      if (list.hidden) {
        open();
      } else {
        close(true);
      }
    });
    button.addEventListener('keydown', function (e) {
      if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(e.key)) {
        e.preventDefault();
        open();
      }
    });

    list.addEventListener('click', function (e) {
      const li = e.target.closest('.select-option');
      if (li) {
        choose(Number(li.dataset.index));
      }
    });
    list.addEventListener('keydown', function (e) {
      const i = items.indexOf(document.activeElement);
      switch (e.key) {
        case 'ArrowDown': e.preventDefault(); move(i, 1); break;
        case 'ArrowUp':   e.preventDefault(); move(i, -1); break;
        case 'Home':      e.preventDefault(); move(-1, 1); break;
        case 'End':       e.preventDefault(); move(items.length, -1); break;
        case 'Enter':
        case ' ':         e.preventDefault(); if (i >= 0) { choose(i); } break;
        case 'Escape':    e.preventDefault(); close(true); break;
        case 'Tab':       close(false); break;
        default: break;
      }
    });

    // 一覧の外を押したら閉じる
    document.addEventListener('pointerdown', function (e) {
      if (!wrap.contains(e.target)) {
        close(false);
      }
    });

    // フォームのリセットや、スクリプトで値が変わったときも表示を合わせる
    select.addEventListener('change', sync);
    sync();
  }

  document.querySelectorAll('select.login-input').forEach(enhance);
})();
