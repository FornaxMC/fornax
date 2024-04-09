# fornax
High performance modern render engine for Minecraft

## Development
1. Clone both this and [gl-wrapper](https://github.com/Luna5ama/gl-wrapper) to the same directory
2. Open project in this repository

## Features (aka TODO list)
- [ ] Terrain draw pipeline
    - [ ] Chunk meshing
        - [ ] Block model loading
        - [ ] Block texture loading
        - [ ] Block meshing
    - [ ] Pre-draw
        - [ ] Frustum culling
        - [ ] Face pre-culling
        - [ ] Occlusion culling
        - [ ] Quad sorting
        - [ ] Chunk sorting
    - [ ] Drawing
        - [ ] Visibility buffer rendering
        - [ ] Regular hardware rasterization
        - [ ] Software rasterization for low LOD chunks
        - [ ] Velocity buffer


- [ ] Entity draw pipeline


- [ ] Other draw pipeline
    - [ ] Volumetric clouds


- [ ] Lighting pipeline
    - [ ] Global lights
    - [ ] Block lights
    - [ ] Shadow mapping
      - [ ] RTWSM (Rectilinear Texture Warping Shadow Maps)
      - [ ] Cloud shadows
      - [ ] RSM (Reflective Shadow Maps)
    - [ ] Deferred lighting
    - [ ] PBR (GGX) direct lighting
    - [ ] RSM GI
    - [ ] [SSILVB](https://arxiv.org/pdf/2301.11376.pdf) + [LSAO](http://wili.cc/research/lsao/lsao.pdf)
    - [ ] Virtual point light GI
    - [ ] Atmospheric scattering (Egor Yusov, High Performance Outdoor Light Scattering Using Epipolar Sampling, GPU Pro 5)


- [ ] Post-processing pipeline
    - [ ] FXAA
    - [ ] TAA
    - [ ] AMD FSR 2.0
    - [ ] AMD CAS
    - [ ] Depth of field
    - [ ] Motion blur
    - [ ] Lens flare
    - [ ] Auto-exposure
    - [ ] Bloom
    - [ ] Tone mapping
    - [ ] Color grading


- [ ] Future API
    - [ ] Extension modding API
    - [ ] Modular render pipeline