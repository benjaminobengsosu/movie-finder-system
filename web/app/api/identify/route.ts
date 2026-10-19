import { NextRequest, NextResponse } from 'next/server';

export async function POST(req: NextRequest) {
  try {
    const { imageBase64, mimeType = 'image/jpeg', contextPrompt = '', userApiKey = '' } = await req.json();

    // Prioritize environment variable, fallback to user-provided key from browser settings
    const geminiKey = process.env.GEMINI_API_KEY || userApiKey;
    if (!geminiKey) {
      return NextResponse.json(
        {
          error: 'Gemini API Key is not set. Add GEMINI_API_KEY to your .env.local (or Vercel Environment Variables), or enter your key in the API Key settings modal.'
        },
        { status: 400 }
      );
    }

    const systemInstruction = `You are a world-class cinematic scene identifier, film historian, and cinema intelligence AI.
Your job is to identify the EXACT movie, film title, or television series from either a visual video frame / picture capture, or a dialogue/scene description.
Analyze all available evidence (actors, setting, cinematography, props, dialogue, mood) to determine the canonical film title, year, director, and IMDb link.`;

    const userPrompt = imageBase64
      ? `Identify the exact movie or television series shown in this picture.
${contextPrompt ? `Additional scene clues / dialogue: "${contextPrompt}".` : ''}

Strictly output your response as valid JSON matching this schema:
{
  "rationale": "Chain-of-thought analysis of actors, visual clues, setting, and costumes",
  "title": "Exact canonical film title",
  "year": "Release year (e.g. 2010)",
  "director": "Director's name",
  "actors": "Comma-separated recognizable actors",
  "genre": "Primary genres",
  "confidence": "High, Medium, or Low",
  "imdbUrl": "Direct valid IMDb URL (https://www.imdb.com/title/tt...)",
  "thematicElements": "Key cinematic themes"
}`
      : `Identify the exact movie or television series from this scene description, plot moment, or dialogue quote:
"${contextPrompt}"

Strictly output your response as valid JSON matching this schema:
{
  "rationale": "Detailed explanation of why this description matches the movie",
  "title": "Exact canonical film title",
  "year": "Release year (e.g. 2010)",
  "director": "Director's name",
  "actors": "Comma-separated recognizable actors",
  "genre": "Primary genres",
  "confidence": "High, Medium, or Low",
  "imdbUrl": "Direct valid IMDb URL (https://www.imdb.com/title/tt...)",
  "thematicElements": "Key cinematic themes"
}`;

    const parts: any[] = [];
    if (imageBase64) {
      const cleanBase64 = imageBase64.replace(/^data:image\/[a-z]+;base64,/, '');
      parts.push({
        inline_data: {
          mime_type: mimeType,
          data: cleanBase64
        }
      });
    }
    parts.push({ text: userPrompt });

    const requestBody = {
      contents: [{ role: 'user', parts }],
      system_instruction: { parts: [{ text: systemInstruction }] },
      generation_config: {
        temperature: 0.2,
        response_mime_type: 'application/json'
      }
    };

    // Attempt primary model: gemini-3.5-flash, fallback to gemini-3.1-pro-preview
    const modelsToTry = ['gemini-3.5-flash', 'gemini-3.1-pro-preview'];
    let lastError = '';
    let candidateText = '';

    for (const model of modelsToTry) {
      try {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${geminiKey}`;
        const response = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(requestBody)
        });

        if (response.ok) {
          const data = await response.json();
          candidateText = data.candidates?.[0]?.content?.parts?.[0]?.text || '';
          if (candidateText) break;
        } else {
          const errBody = await response.text();
          lastError = `HTTP ${response.status}: ${errBody}`;
        }
      } catch (e: any) {
        lastError = e.message;
      }
    }

    if (!candidateText) {
      return NextResponse.json(
        { error: `Could not reach Gemini models. Details: ${lastError}` },
        { status: 502 }
      );
    }

    let resultJson: any;
    try {
      // Strip markdown code fences if present
      const cleanJson = candidateText.replace(/^```json\s*/i, '').replace(/```\s*$/i, '').trim();
      resultJson = JSON.parse(cleanJson);
    } catch {
      return NextResponse.json({ error: 'Failed to parse JSON response from Gemini model.' }, { status: 500 });
    }

    // Optional TMDB Poster enrichment
    const tmdbKey = process.env.TMDB_API_KEY;
    if (tmdbKey && resultJson.title && resultJson.title.toLowerCase() !== 'unknown') {
      try {
        const tmdbRes = await fetch(
          `https://api.themoviedb.org/3/search/multi?api_key=${tmdbKey}&query=${encodeURIComponent(resultJson.title)}`
        );
        if (tmdbRes.ok) {
          const tmdbData = await tmdbRes.json();
          const match = tmdbData.results?.find((r: any) => r.poster_path);
          if (match?.poster_path) {
            resultJson.poster_path = `https://image.tmdb.org/t/p/w500${match.poster_path}`;
          }
        }
      } catch (e) {
        console.warn('TMDB enrichment skipped:', e);
      }
    }

    return NextResponse.json(resultJson);

  } catch (error: any) {
    return NextResponse.json({ error: error.message || 'Scene processing failed' }, { status: 500 });
  }
}
