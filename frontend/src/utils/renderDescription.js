import { marked } from 'marked'
import katex from 'katex'
import 'katex/dist/katex.min.css'

/**
 * 문제 본문(description)을 화면에 표시할 HTML로 변환한다.
 *
 * 사이트마다 본문 형식이 완전히 다르므로 site 로 렌더 경로를 나눈다.
 *   - BOJ(백준): 이미 HTML(<p>, <ol>, <table>…) + 헤더 마크다운(### 입력). LaTeX 없음(수식은 MathJax/이미지).
 *   - CODEFORCES: 마크다운(**Input**) + $$$...$$$ 삼중 달러 LaTeX. HTML 태그 없음.
 *
 * 그래서 BOJ 경로(renderBoj)는 기존 그대로 두고, 코드포스만 별도 경로(renderCodeforces)로 처리한다.
 * → 코드포스 렌더를 손봐도 백준 본문은 영향받지 않는다.
 */

// ── BOJ가 미리 렌더링해 둔 MathJax HTML을 원본 $..$ LaTeX로 되돌린다 ──
// <mjx-container> 안 <span class="mjx-copytext">에 원본 수식이 들어있다.
function restoreMathJaxSource(text) {
  if (!text.includes('mjx-container')) return text
  return text.replace(
    /<mjx-container[^>]*>[\s\S]*?<span[^>]*mjx-copytext[^>]*>([\s\S]*?)<\/span>\s*<\/mjx-container>/g,
    (_, latex) => latex
  )
}

// ── LaTeX를 KaTeX로 렌더링한다 ──
// 코드포스의 $$$...$$$(삼중)를 먼저 소비해야 한다. 안 그러면 $$ / $ 규칙이 삼중 달러를
// 어긋나게 끊어 수식 경계가 prose까지 번지고(KaTeX 파싱 에러의 원인) 만다.
function renderMath(text) {
  if (!text.includes('$')) return text

  // 코드포스 디스플레이 수식: $$$$$$...$$$$$$ (6달러) — 3달러보다 "먼저" 소비해야 어긋나지 않는다
  text = text.replace(/\$\$\$\$\$\$([\s\S]+?)\$\$\$\$\$\$/g, (whole, tex) => {
    try {
      return katex.renderToString(tex.trim(), { displayMode: true, throwOnError: false })
    } catch {
      return whole
    }
  })

  // 코드포스 인라인 수식: $$$...$$$ (3달러)
  text = text.replace(/\$\$\$([\s\S]+?)\$\$\$/g, (whole, tex) => {
    try {
      return katex.renderToString(tex.trim(), { displayMode: false, throwOnError: false })
    } catch {
      return whole
    }
  })

  // $$..$$ (블록)
  text = text.replace(/\$\$([\s\S]*?)\$\$/g, (whole, tex) => {
    try {
      return katex.renderToString(tex.trim(), { displayMode: true, throwOnError: false })
    } catch {
      return whole
    }
  })

  // $..$ (인라인)
  text = text.replace(/\$([^$\n]+?)\$/g, (whole, tex) => {
    try {
      return katex.renderToString(tex.trim(), { displayMode: false, throwOnError: false })
    } catch {
      return whole
    }
  })

  return text
}

// ── HTML 본문 처리: 헤더(### …)만 <h3> 등으로 변환, 나머지 HTML은 원형 보존 ──
// (HTML을 통째로 marked에 넣으면 빈 줄·탭을 마크다운으로 재해석해 <ol>/<li> 구조가 붕괴된다.)
function renderContent(text) {
  if (containsBlockHtml(text)) {
    return convertHeadersOnly(text)
  }
  return marked.parse(text)
}

function containsBlockHtml(text) {
  return /<(?:p|ol|ul|li|table|div|blockquote|pre|h[1-6])\b/i.test(text)
}

function convertHeadersOnly(text) {
  return text.replace(/^[ \t]*(#{1,6})[ \t]+(.+?)[ \t]*$/gm, (_, hashes, title) => {
    const level = hashes.length
    return `<h${level}>${title}</h${level}>`
  })
}

// ── 백준 등 HTML 본문용 (기존 동작 그대로) ──
function renderBoj(raw) {
  let html = raw
  html = restoreMathJaxSource(html)
  html = renderMath(html)
  html = renderContent(html)
  return html
}

// ── 코드포스용: $$$LaTeX$$$ → KaTeX 먼저, 그다음 마크다운(**Input** 등) ──
// 수식을 먼저 HTML로 바꿔야 marked 가 LaTeX 안의 \, <, & 등을 escape 해서 깨뜨리지 않는다.
function renderCodeforces(raw) {
  let html = renderMath(raw)
  html = marked.parse(html)
  return html
}

/**
 * @param {string} raw  - DB에 저장된 원본 본문
 * @param {string} site - 'BOJ' | 'CODEFORCES' (없으면 BOJ 경로로 처리)
 * @returns {string} 화면에 v-html로 꽂을 HTML
 */
export function renderDescription(raw, site) {
  if (!raw) return ''
  if (site === 'CODEFORCES') {
    return renderCodeforces(raw)
  }
  return renderBoj(raw)
}
