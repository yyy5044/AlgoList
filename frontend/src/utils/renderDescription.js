import { marked } from 'marked'
import katex from 'katex'
import 'katex/dist/katex.min.css'

/**
 * 문제 본문(description)을 화면에 표시할 HTML로 변환한다.
 *
 * ── 설계 배경 ──
 * 백준허브가 수집한 본문은 "이미 HTML"이다(<p>, <ol>, <li>, <table>…).
 * 거기에 섞인 마크다운은 `### 입력` 같은 헤더 몇 개가 전부다.
 *
 * 이 HTML을 통째로 마크다운 파서(marked)에 넣으면, 파서가 HTML 속
 * 들여쓰기(탭)·빈 줄을 마크다운 문법으로 재해석해 버린다:
 *   - 빈 줄 → HTML 블록을 끊음 → <ol>과 <li>가 분리되어 중첩 구조 붕괴
 *   - 탭    → 들여쓴 코드 블록으로 인식 → 태그가 그대로 노출
 * 따라서 HTML 본문에는 마크다운 파서를 "쓰지 않는다". 유일한 마크다운인
 * 헤더만 직접 변환하고, 나머지 HTML은 손대지 않고 그대로 둔다.
 *
 * 순수 마크다운만 들어오는 사이트를 위해 marked 경로도 남겨두되,
 * HTML이 감지되면 그 경로로 가지 않는다.
 *
 * 파이프라인: MathJax 복원 → LaTeX 렌더링 → (HTML이면) 헤더만 변환 / (마크다운이면) marked
 */

// ── 1단계: 백준이 미리 렌더링해 둔 MathJax HTML을 원본 $..$ LaTeX로 되돌린다 ──
// <mjx-container> 안 <span class="mjx-copytext">에 원본 수식이 들어있다.
function restoreMathJaxSource(text) {
  if (!text.includes('mjx-container')) return text
  return text.replace(
    /<mjx-container[^>]*>[\s\S]*?<span[^>]*mjx-copytext[^>]*>([\s\S]*?)<\/span>\s*<\/mjx-container>/g,
    (_, latex) => latex
  )
}

// ── 2단계: $$..$$(블록), $..$(인라인) LaTeX를 KaTeX로 렌더링한다 ──
function renderMath(text) {
  if (!text.includes('$')) return text

  text = text.replace(/\$\$([\s\S]*?)\$\$/g, (whole, tex) => {
    try {
      return katex.renderToString(tex.trim(), { displayMode: true, throwOnError: false })
    } catch {
      return whole
    }
  })

  text = text.replace(/\$([^$\n]+?)\$/g, (whole, tex) => {
    try {
      return katex.renderToString(tex.trim(), { displayMode: false, throwOnError: false })
    } catch {
      return whole
    }
  })

  return text
}

// ── 3단계: 콘텐츠 성격에 맞게 변환한다 ──
//   HTML 본문   → 헤더(### …)만 <h3> 등으로 변환, 나머지 HTML은 원형 보존
//   순수 마크다운 → marked로 전체 변환
function renderContent(text) {
  if (containsBlockHtml(text)) {
    return convertHeadersOnly(text)
  }
  return marked.parse(text)
}

// 블록 레벨 HTML 태그가 하나라도 있으면 "HTML 본문"으로 본다.
function containsBlockHtml(text) {
  return /<(?:p|ol|ul|li|table|div|blockquote|pre|h[1-6])\b/i.test(text)
}

// 한 줄을 통째로 차지하는 `### 제목`만 헤더로 변환한다.
function convertHeadersOnly(text) {
  return text.replace(/^[ \t]*(#{1,6})[ \t]+(.+?)[ \t]*$/gm, (_, hashes, title) => {
    const level = hashes.length
    return `<h${level}>${title}</h${level}>`
  })
}

/**
 * @param {string} raw - DB에 저장된 원본 본문
 * @returns {string} 화면에 v-html로 꽂을 HTML
 */
export function renderDescription(raw) {
  if (!raw) return ''
  let html = raw
  html = restoreMathJaxSource(html)
  html = renderMath(html)
  html = renderContent(html)
  return html
}
