const { useState, useRef, useEffect } = React;

function JavaEditor() {
  const editorDivRef = useRef(null);
  const editorInstanceRef = useRef(null);
  const monacoRef = useRef(null);

  const defaultCode = 'public class Main {\n  public static void main(String[] args) {\n    System.out.println("Hello World");\n  }\n}';

  useEffect(() => {
    // Configure the AMD loader to point to the Monaco CDN
    window.require.config({ paths: { 'vs': 'https://unpkg.com/monaco-editor@0.47.0/min/vs' }});

    // Load the main editor module
    window.require(['vs/editor/editor.main'], () => {
      const monaco = window.monaco;
      monacoRef.current = monaco;

      // Create the editor instance and attach it to the div
      editorInstanceRef.current = monaco.editor.create(editorDivRef.current, {
        value: defaultCode,
        language: 'java',
        theme: 'vs-dark',
        minimap: { enabled: false }
      });
    });

    // Cleanup function to destroy the editor when the component unmounts
    return () => {
      if (editorInstanceRef.current) {
        editorInstanceRef.current.dispose();
      }
    };
  }, []);

   async function fetchResponse(code) {
     const response = await fetch('/compile', {
       method: 'POST',
       headers: {
         'Content-Type': 'application/json'
       },
       body: JSON.stringify({ code })
     });

     if (!response.ok) {
       throw new Error(response.statusText);
     }

     return response.json();
   }

  async function compileCode() {
    if (!editorInstanceRef.current || !monacoRef.current) return;

    const currentCode = editorInstanceRef.current.getValue();
    const monaco = monacoRef.current;
    const model = editorInstanceRef.current.getModel();

    try {
      const errors = await fetchResponse(currentCode);

      if (errors.length === 0) {
        alert("Compilation successful!");
        monaco.editor.setModelMarkers(model, 'java-compiler', []);
        return;
      }

      // Map Java diagnostics to Monaco markers
      const markers = errors.map(err => ({
        severity: monaco.MarkerSeverity.Error,
        startLineNumber: err.line,
        startColumn: err.column,
        endLineNumber: err.line,
        endColumn: err.column + 1, // Highlight the character token
        message: err.message
      }));

      // Apply red squiggles
      monaco.editor.setModelMarkers(model, 'java-compiler', markers);

    } catch (err) {
      console.error('Compilation fetch failed', err);
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <div style={{ padding: '10px', backgroundColor: '#202124', borderBottom: '1px solid #333' }}>
        <button
          onClick={compileCode}
          style={{ padding: '8px 16px', fontSize: '14px', cursor: 'pointer', backgroundColor: '#4CAF50', color: 'white', border: 'none', borderRadius: '4px' }}>
          Compile Code
        </button>
      </div>
      {/* The div where Monaco will inject itself */}
      <div ref={editorDivRef} style={{ flexGrow: 1 }} />
    </div>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<JavaEditor />);